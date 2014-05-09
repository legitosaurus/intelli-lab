# IntelliLab

IntelliLab is an extension for IntelliJ IDEA which allows for seamlessly integrating GitLab issues with IntelliJ tasks.


## System requirements

* IntelliJ IDEA 13 or later (community or ultimate edition)
* Java 8 or later (see motivation below)


## Usage

First of all, a link needs to be forged between the currently open IntelliJ project and an arbitrary GitLab project
(which typically also manages the source of this IntelliJ project). After that, all GitLab issues are loaded and
displayed in a special tool window (reasonably called GitLab Issues). It offers the opportunity to create new or modify
existing issues. Deleting issues is not possible since GitLab does not support it as well.

In addition to GitLab, a third issue state has been added which we call "active". This state is just managed locally,
i.e. the GitLab server does not know anything about it. Here, the linkage of GitLab issues and IntelliJ local tasks
comes into play: the first time an issue is started, a new local task is created and activated (which also activates
the associated working context). If an active issue is stopped the default task is activated but the linkage between
issue and local task remains. In case an issue is started again the already linked task is re-activated. The local task
is removed only after the linked issue has been closed.

For an exhaustive list of all features, see the [changelog](CHANGELOG.md).


## Motivation behind this project

The main motivation behind this project was that I wanted to play a bit with the new features of Java 8. Furthermore,
I also contemplated to learn something about plugin development for the IntelliJ ecosystem. So, I decided to combine
both and developed an IntelliJ plugin using Java 8.

Since at work we are using IntelliJ along with GitLab, I thought it would be nice to have a seamless integration of
IntelliJ tasks and GitLab issues as described above. Accordingly, I built this plugin that henceforth simplifies my
daily software development workflow.

Now, you know the circumstances under which this plugin has been emerged. Due to that, there is undoubtedly some
potential for improvements. If you find something of the sort feel free to open an according issue.
