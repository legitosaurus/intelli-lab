package eu.broth.intellilab.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.net.ssl.CertificatesManager;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Bastian Roth
 * @version 02.04.2014
 */
public class RestUtil {

	private static final int TIMEOUT = 5000;

	public static interface AsyncCallback {
		void onSuccess(String response);
	}

	public static boolean GET(@NotNull String url, @NotNull String token, String message, AsyncCallback callback) {
		HttpGet request = new HttpGet(url);
		return send(request, token, message, callback);
	}

	public static boolean PUT(@NotNull String url, @NotNull String token, @NotNull Params params, String message,
							  AsyncCallback callback) {
		HttpPut request = new HttpPut(url);
		try {
			addDataToRequest(request, params);
			return send(request, token, message, callback);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean POST(@NotNull String url, @NotNull String token, @NotNull Params params, String message,
							  AsyncCallback callback) {
		HttpPost request = new HttpPost(url);
		try {
			addDataToRequest(request, params);
			return send(request, token, message, callback);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private static void addDataToRequest(@NotNull HttpEntityEnclosingRequestBase request, @NotNull Params params)
			throws UnsupportedEncodingException {
		String data = params.format();
		StringEntity entity = new StringEntity(data);
		entity.setContentType("application/x-www-form-urlencoded");
		request.setEntity(entity);
	}

	private static <Req extends HttpUriRequest> boolean send(@NotNull Req request, @NotNull String token,
															 String message, AsyncCallback callback) {
		request.addHeader("PRIVATE-TOKEN", token);
		request.addHeader("Accept-Charset", "utf-8");
		RestTask<Req> task = new RestTask(request, message, callback);
		ProgressManager.getInstance().run(task);
		if (task.exception != null) {
			if (!(task.exception instanceof ProcessCanceledException)) {
				Messages.showErrorDialog((Project) null, task.exception.getMessage(), "Connection error");
			}
			return false;
		}
		return true;
	}

	private static HttpClient newClient() {
		RequestConfig config = RequestConfig.custom()
				.setConnectTimeout(TIMEOUT)
				.setSocketTimeout(TIMEOUT)
				.setConnectionRequestTimeout(TIMEOUT)
				.build();
		HttpClientBuilder builder = HttpClients.custom()
				.setSslcontext(CertificatesManager.getInstance().getSslContext())
				.setHostnameVerifier((X509HostnameVerifier) CertificatesManager.HOSTNAME_VERIFIER)
				.setDefaultRequestConfig(config);
		return builder.build();
	}


	private static class RestTask<Req extends HttpUriRequest> extends Task.Modal {

		private final Req request;
		private final String message;
		private final AsyncCallback callback;

		Exception exception;

		public RestTask(@NotNull Req request, String message, AsyncCallback callback) {
			super(null, "Contacting GitLab server", true);
			this.request = request;
			this.message = message;
			this.callback = callback;
		}

		@Override
		public void run(@NotNull ProgressIndicator indicator) {
			if (message != null) {
				indicator.setText(message);
			}
			indicator.setFraction(0);
			indicator.setIndeterminate(true);

			HttpClient client = newClient();
			try {
				Future<Exception> future = ApplicationManager.getApplication().executeOnPooledThread(() -> {
					try {
						HttpResponse httpResponse = client.execute(request);
						final StatusLine statusLine = httpResponse.getStatusLine();
						if (statusLine.getStatusCode() >= 300) {
							EntityUtils.consume(httpResponse.getEntity());
							throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
						}
						String response = EntityUtils.toString(httpResponse.getEntity(), "utf-8");
						if (callback != null) {
							callback.onSuccess(response);
						}
					} catch (Exception ex) {
						return ex;
					}
					return null;
				});
				while (true) {
					try {
						exception = future.get(100, TimeUnit.MILLISECONDS);
						return;
					} catch (TimeoutException ignore) {
						try {
							indicator.checkCanceled();
						} catch (ProcessCanceledException pce) {
							exception = pce;
							request.abort();
							return;
						}
					} catch (Exception e) {
						exception = e;
						return;
					}
				}
			} catch (Exception e) {
				this.exception = e;
			}
		}

		@Override
		public void onCancel() {
			request.abort();
		}
	}
}
