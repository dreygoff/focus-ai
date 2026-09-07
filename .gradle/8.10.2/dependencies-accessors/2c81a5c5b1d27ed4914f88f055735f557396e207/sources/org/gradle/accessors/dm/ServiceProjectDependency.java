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
public class ServiceProjectDependency extends DelegatingProjectDependency {

    @Inject
    public ServiceProjectDependency(TypeSafeProjectDependencyFactory factory, ProjectDependencyInternal delegate) {
        super(factory, delegate);
    }

    /**
     * Creates a project dependency on the project at path ":service:accessibility"
     */
    public Service_AccessibilityProjectDependency getAccessibility() { return new Service_AccessibilityProjectDependency(getFactory(), create(":service:accessibility")); }

    /**
     * Creates a project dependency on the project at path ":service:focus-service"
     */
    public Service_FocusServiceProjectDependency getFocusService() { return new Service_FocusServiceProjectDependency(getFactory(), create(":service:focus-service")); }

}
