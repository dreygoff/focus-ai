package org.gradle.accessors.dm;

import org.gradle.api.NonNullApi;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.internal.artifacts.dependencies.ProjectDependencyInternal;
import org.gradle.api.internal.artifacts.DefaultProjectDependencyFactory;
import org.gradle.api.internal.artifacts.dsl.dependencies.ProjectFinder;
import org.gradle.api.internal.catalog.DelegatingProjectDependency;
import org.gradle.api.internal.catalog.TypeSafeProjectDependencyFactory;
import javax.inject.Inject;

@NonNullApi
public class FeatureProjectDependency extends DelegatingProjectDependency {

    @Inject
    public FeatureProjectDependency(TypeSafeProjectDependencyFactory factory, ProjectDependencyInternal delegate) {
        super(factory, delegate);
    }

    /**
     * Creates a project dependency on the project at path ":feature:blocker"
     */
    public Feature_BlockerProjectDependency getBlocker() { return new Feature_BlockerProjectDependency(getFactory(), create(":feature:blocker")); }

    /**
     * Creates a project dependency on the project at path ":feature:home"
     */
    public Feature_HomeProjectDependency getHome() { return new Feature_HomeProjectDependency(getFactory(), create(":feature:home")); }

    /**
     * Creates a project dependency on the project at path ":feature:onboarding"
     */
    public Feature_OnboardingProjectDependency getOnboarding() { return new Feature_OnboardingProjectDependency(getFactory(), create(":feature:onboarding")); }

    /**
     * Creates a project dependency on the project at path ":feature:permissions"
     */
    public Feature_PermissionsProjectDependency getPermissions() { return new Feature_PermissionsProjectDependency(getFactory(), create(":feature:permissions")); }

    /**
     * Creates a project dependency on the project at path ":feature:profiles"
     */
    public Feature_ProfilesProjectDependency getProfiles() { return new Feature_ProfilesProjectDependency(getFactory(), create(":feature:profiles")); }

    /**
     * Creates a project dependency on the project at path ":feature:schedules"
     */
    public Feature_SchedulesProjectDependency getSchedules() { return new Feature_SchedulesProjectDependency(getFactory(), create(":feature:schedules")); }

    /**
     * Creates a project dependency on the project at path ":feature:session"
     */
    public Feature_SessionProjectDependency getSession() { return new Feature_SessionProjectDependency(getFactory(), create(":feature:session")); }

    /**
     * Creates a project dependency on the project at path ":feature:settings"
     */
    public Feature_SettingsProjectDependency getSettings() { return new Feature_SettingsProjectDependency(getFactory(), create(":feature:settings")); }

    /**
     * Creates a project dependency on the project at path ":feature:stats"
     */
    public Feature_StatsProjectDependency getStats() { return new Feature_StatsProjectDependency(getFactory(), create(":feature:stats")); }

    /**
     * Creates a project dependency on the project at path ":feature:widget"
     */
    public Feature_WidgetProjectDependency getWidget() { return new Feature_WidgetProjectDependency(getFactory(), create(":feature:widget")); }

}
