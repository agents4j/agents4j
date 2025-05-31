package dev.agents4j.api.context;

import java.security.Principal;
import java.util.*;

/**
 * Specialized context for security-related operations.
 * Manages authentication, authorization, and security constraints.
 */
public final class SecurityContext implements WorkflowContext {
    
    private final WorkflowContext delegate;
    private final Optional<Principal> principal;
    private final Set<String> roles;
    private final Set<String> permissions;
    private final Map<String, Object> securityAttributes;
    
    private SecurityContext(WorkflowContext delegate, Principal principal, 
                           Set<String> roles, Set<String> permissions,
                           Map<String, Object> securityAttributes) {
        this.delegate = Objects.requireNonNull(delegate, "Delegate context cannot be null");
        this.principal = Optional.ofNullable(principal);
        this.roles = Collections.unmodifiableSet(new HashSet<>(roles));
        this.permissions = Collections.unmodifiableSet(new HashSet<>(permissions));
        this.securityAttributes = Collections.unmodifiableMap(new HashMap<>(securityAttributes));
    }
    
    /**
     * Creates an empty security context (unauthenticated).
     *
     * @return An empty SecurityContext
     */
    public static SecurityContext empty() {
        return new SecurityContext(ExecutionContext.empty(), null, 
            Collections.emptySet(), Collections.emptySet(), Collections.emptyMap());
    }
    
    /**
     * Creates a security context from an existing workflow context.
     *
     * @param context The base context
     * @return A new SecurityContext wrapping the provided context
     */
    public static SecurityContext from(WorkflowContext context) {
        return new SecurityContext(context, null, 
            Collections.emptySet(), Collections.emptySet(), Collections.emptyMap());
    }
    
    /**
     * Creates a security context with a principal.
     *
     * @param context The base context
     * @param principal The authenticated principal
     * @return A new SecurityContext with the principal
     */
    public static SecurityContext withPrincipal(WorkflowContext context, Principal principal) {
        return new SecurityContext(context, principal, 
            Collections.emptySet(), Collections.emptySet(), Collections.emptyMap());
    }
    
    /**
     * Creates a security context with authentication details.
     *
     * @param context The base context
     * @param principal The authenticated principal
     * @param roles The user's roles
     * @param permissions The user's permissions
     * @return A new SecurityContext with authentication details
     */
    public static SecurityContext authenticated(WorkflowContext context, Principal principal,
                                              Set<String> roles, Set<String> permissions) {
        return new SecurityContext(context, principal, roles, permissions, Collections.emptyMap());
    }
    
    @Override
    public <T> Optional<T> get(ContextKey<T> key) {
        return delegate.get(key);
    }
    
    @Override
    public <T> WorkflowContext with(ContextKey<T> key, T value) {
        return new SecurityContext(delegate.with(key, value), principal.orElse(null), 
            roles, permissions, securityAttributes);
    }
    
    @Override
    public WorkflowContext without(ContextKey<?> key) {
        return new SecurityContext(delegate.without(key), principal.orElse(null), 
            roles, permissions, securityAttributes);
    }
    
    @Override
    public boolean contains(ContextKey<?> key) {
        return delegate.contains(key);
    }
    
    @Override
    public Set<ContextKey<?>> keys() {
        return delegate.keys();
    }
    
    @Override
    public int size() {
        return delegate.size();
    }
    
    @Override
    public WorkflowContext merge(WorkflowContext other) {
        if (other instanceof SecurityContext securityOther) {
            // Security contexts cannot be simply merged - need explicit handling
            throw new UnsupportedOperationException(
                "Security contexts require explicit merge strategy for authentication data");
        } else {
            return new SecurityContext(delegate.merge(other), principal.orElse(null), 
                roles, permissions, securityAttributes);
        }
    }
    
    /**
     * Sets the authenticated principal.
     *
     * @param principal The principal to set
     * @return A new SecurityContext with the principal
     */
    public SecurityContext withPrincipal(Principal principal) {
        return new SecurityContext(delegate, principal, roles, permissions, securityAttributes);
    }
    
    /**
     * Adds a role to this security context.
     *
     * @param role The role to add
     * @return A new SecurityContext with the role added
     */
    public SecurityContext withRole(String role) {
        Objects.requireNonNull(role, "Role cannot be null");
        Set<String> newRoles = new HashSet<>(roles);
        newRoles.add(role);
        return new SecurityContext(delegate, principal.orElse(null), newRoles, permissions, securityAttributes);
    }
    
    /**
     * Adds multiple roles to this security context.
     *
     * @param newRoles The roles to add
     * @return A new SecurityContext with the roles added
     */
    public SecurityContext withRoles(Collection<String> newRoles) {
        Objects.requireNonNull(newRoles, "Roles collection cannot be null");
        Set<String> allRoles = new HashSet<>(roles);
        allRoles.addAll(newRoles);
        return new SecurityContext(delegate, principal.orElse(null), allRoles, permissions, securityAttributes);
    }
    
    /**
     * Adds a permission to this security context.
     *
     * @param permission The permission to add
     * @return A new SecurityContext with the permission added
     */
    public SecurityContext withPermission(String permission) {
        Objects.requireNonNull(permission, "Permission cannot be null");
        Set<String> newPermissions = new HashSet<>(permissions);
        newPermissions.add(permission);
        return new SecurityContext(delegate, principal.orElse(null), roles, newPermissions, securityAttributes);
    }
    
    /**
     * Adds multiple permissions to this security context.
     *
     * @param newPermissions The permissions to add
     * @return A new SecurityContext with the permissions added
     */
    public SecurityContext withPermissions(Collection<String> newPermissions) {
        Objects.requireNonNull(newPermissions, "Permissions collection cannot be null");
        Set<String> allPermissions = new HashSet<>(permissions);
        allPermissions.addAll(newPermissions);
        return new SecurityContext(delegate, principal.orElse(null), roles, allPermissions, securityAttributes);
    }
    
    /**
     * Adds a security attribute.
     *
     * @param key The attribute key
     * @param value The attribute value
     * @return A new SecurityContext with the attribute added
     */
    public SecurityContext withSecurityAttribute(String key, Object value) {
        Objects.requireNonNull(key, "Security attribute key cannot be null");
        Map<String, Object> newAttributes = new HashMap<>(securityAttributes);
        newAttributes.put(key, value);
        return new SecurityContext(delegate, principal.orElse(null), roles, permissions, newAttributes);
    }
    
    /**
     * Gets the authenticated principal.
     *
     * @return The principal if authenticated
     */
    public Optional<Principal> getPrincipal() {
        return principal;
    }
    
    /**
     * Gets the user's roles.
     *
     * @return An unmodifiable set of roles
     */
    public Set<String> getRoles() {
        return roles;
    }
    
    /**
     * Gets the user's permissions.
     *
     * @return An unmodifiable set of permissions
     */
    public Set<String> getPermissions() {
        return permissions;
    }
    
    /**
     * Gets security attributes.
     *
     * @return An unmodifiable map of security attributes
     */
    public Map<String, Object> getSecurityAttributes() {
        return securityAttributes;
    }
    
    /**
     * Checks if the user is authenticated.
     *
     * @return true if a principal is present
     */
    public boolean isAuthenticated() {
        return principal.isPresent();
    }
    
    /**
     * Checks if the user has a specific role.
     *
     * @param role The role to check
     * @return true if the user has the role
     */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }
    
    /**
     * Checks if the user has any of the specified roles.
     *
     * @param rolesToCheck The roles to check
     * @return true if the user has at least one of the roles
     */
    public boolean hasAnyRole(Collection<String> rolesToCheck) {
        return rolesToCheck.stream().anyMatch(roles::contains);
    }
    
    /**
     * Checks if the user has all of the specified roles.
     *
     * @param rolesToCheck The roles to check
     * @return true if the user has all of the roles
     */
    public boolean hasAllRoles(Collection<String> rolesToCheck) {
        return roles.containsAll(rolesToCheck);
    }
    
    /**
     * Checks if the user has a specific permission.
     *
     * @param permission The permission to check
     * @return true if the user has the permission
     */
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }
    
    /**
     * Checks if the user has any of the specified permissions.
     *
     * @param permissionsToCheck The permissions to check
     * @return true if the user has at least one of the permissions
     */
    public boolean hasAnyPermission(Collection<String> permissionsToCheck) {
        return permissionsToCheck.stream().anyMatch(permissions::contains);
    }
    
    /**
     * Checks if the user has all of the specified permissions.
     *
     * @param permissionsToCheck The permissions to check
     * @return true if the user has all of the permissions
     */
    public boolean hasAllPermissions(Collection<String> permissionsToCheck) {
        return permissions.containsAll(permissionsToCheck);
    }
    
    /**
     * Gets a security attribute value.
     *
     * @param key The attribute key
     * @param <T> The expected type
     * @return The attribute value
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getSecurityAttribute(String key) {
        return Optional.ofNullable((T) securityAttributes.get(key));
    }
    
    /**
     * Creates an unauthenticated security context.
     *
     * @return A new SecurityContext without authentication
     */
    public SecurityContext unauthenticated() {
        return new SecurityContext(delegate, null, Collections.emptySet(), 
            Collections.emptySet(), Collections.emptyMap());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SecurityContext that = (SecurityContext) o;
        return Objects.equals(delegate, that.delegate) &&
               Objects.equals(principal, that.principal) &&
               Objects.equals(roles, that.roles) &&
               Objects.equals(permissions, that.permissions) &&
               Objects.equals(securityAttributes, that.securityAttributes);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(delegate, principal, roles, permissions, securityAttributes);
    }
    
    @Override
    public String toString() {
        return String.format("SecurityContext{size=%d, authenticated=%s, roles=%d, permissions=%d}", 
            delegate.size(), isAuthenticated(), roles.size(), permissions.size());
    }
}