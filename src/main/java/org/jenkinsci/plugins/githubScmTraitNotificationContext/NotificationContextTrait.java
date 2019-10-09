package org.jenkinsci.plugins.githubScmTraitNotificationContext;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.TaskListener;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadCategory;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.trait.SCMBuilder;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.api.trait.SCMSourceTraitDescriptor;
import org.jenkinsci.plugins.github_branch_source.*;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class NotificationContextTrait extends SCMSourceTrait {

    private String contextLabel;
    private boolean typeSuffix;
    private boolean sendCustomURL;
	private String urlLabel;

    @DataBoundConstructor
    public NotificationContextTrait(String contextLabel, boolean typeSuffix, boolean sendCustomURL, String urlLabel) {
        this.contextLabel = contextLabel;
        this.typeSuffix = typeSuffix;
        this.sendCustomURL = sendCustomURL;
		this.urlLabel = urlLabel;
    }

    public String getContextLabel() {
        return contextLabel;
    }

    public boolean isTypeSuffix() {
        return typeSuffix;
    }

    public boolean isSendCustomURL() { return sendCustomURL; }

	public String getUrlLabel() {
		return urlLabel;
	}

    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        GitHubSCMSourceContext githubContext = (GitHubSCMSourceContext) context;
        githubContext.withNotificationStrategies(Collections.singletonList(
                new CustomContextNotificationStrategy(contextLabel, typeSuffix, sendCustomURL, urlLabel)));
    }

    @Override
    public boolean includeCategory(@NonNull SCMHeadCategory category) {
        return category.isUncategorized();
    }

    @Extension
    public static class DescriptorImpl extends SCMSourceTraitDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return "Custom Github Notification Context";
        }

        @Override
        public Class<? extends SCMBuilder> getBuilderClass() {
            return GitHubSCMBuilder.class;
        }

        @Override
        public Class<? extends SCMSourceContext> getContextClass() {
            return GitHubSCMSourceContext.class;
        }

        @Override
        public Class<? extends SCMSource> getSourceClass() {
            return GitHubSCMSource.class;
        }
    }

    private static final class CustomContextNotificationStrategy extends AbstractGitHubNotificationStrategy {

        private String contextLabel;
        private boolean typeSuffix;
        private boolean sendCustomURL;
		private String urlLabel;

        CustomContextNotificationStrategy(String contextLabel, boolean typeSuffix, boolean sendCustomURL, String urlLabel) {
            this.contextLabel = contextLabel;
            this.typeSuffix = typeSuffix;
            this.sendCustomURL = sendCustomURL;
			this.urlLabel = urlLabel;
        }

        private String buildContext(GitHubNotificationContext notificationContext) {
            SCMHead head = notificationContext.getHead();
            if (typeSuffix) {
                if (head instanceof PullRequestSCMHead) {
                    if (((PullRequestSCMHead) head).isMerge()) {
                        return contextLabel + "/pr-merge";
                    } else {
                        return contextLabel + "/pr-head";
                    }
                } else {
                    return contextLabel + "/branch";
                }
            }
            return contextLabel;
        }
        
        private String buildURL(GitHubNotificationContext notificationContext, TaskListener listener) {
            if(sendCustomURL) {
                return urlLabel;
            } else {
                return notificationContext.getDefaultUrl(listener);
            }
        }

        @Override
        public List<GitHubNotificationRequest> notifications(GitHubNotificationContext notificationContext, TaskListener listener) {
            return Collections.singletonList(GitHubNotificationRequest.build(buildContext(notificationContext),
                    buildURL(notificationContext, listener),
                    notificationContext.getDefaultMessage(listener),
                    notificationContext.getDefaultState(listener),
                    notificationContext.getDefaultIgnoreError(listener)));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CustomContextNotificationStrategy that = (CustomContextNotificationStrategy) o;
            return typeSuffix == that.typeSuffix &&
                    Objects.equals(contextLabel, that.contextLabel) &&
                    Objects.equals(urlLabel, that.urlLabel);
        }

        @Override
        public int hashCode() {
            return Objects.hash(contextLabel, typeSuffix, urlLabel);
        }
    }
}
