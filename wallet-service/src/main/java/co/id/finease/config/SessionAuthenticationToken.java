package co.id.finease.config;

import co.id.finease.entity.Session;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class SessionAuthenticationToken extends AbstractAuthenticationToken {
    private final Session session;

    public SessionAuthenticationToken(Session session, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.session = session;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return session.getSessionId();
    }

    @Override
    public Object getPrincipal() {
        return session;
    }

    public Session getSession() {
        return session;
    }
}
