package com.ilonggobistro.auth;

import com.ilonggobistro.admin.AdminUser;
import com.ilonggobistro.admin.AdminUserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Authorizes Google OAuth2 sign-ins ONLY when the verified Google email matches
 * an enabled staff account already registered in admin_users.
 */
@Service
public class StaffOidcUserService extends OidcUserService {

    private final AdminUserRepository userRepository;

    public StaffOidcUserService(AdminUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        return authorizeRegisteredStaff(oidcUser);
    }

    public OidcUser authorizeRegisteredStaff(OidcUser oidcUser) {
        String email = oidcUser.getEmail();
        Boolean emailVerified = oidcUser.getEmailVerified();

        if (email == null || email.isBlank() || Boolean.FALSE.equals(emailVerified)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("unverified_email", "Your Google account email must be verified before signing in.", null));
        }

        AdminUser staff = userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new OAuth2AuthenticationException(
                        new OAuth2Error("unregistered_staff",
                                "Google account \"" + email + "\" is not registered in Ilonggo Bistro Staff Directory. Ask the Manager to register your Gmail first.",
                                null)));

        if (!staff.isEnabled()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("disabled_staff", "This staff account has been disabled by the Manager.", null));
        }

        if (!staff.isEmailVerified()) {
            staff.setEmailVerified(true);
            userRepository.save(staff);
        }

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + staff.getRole()));
        return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo(), "email");
    }
}
