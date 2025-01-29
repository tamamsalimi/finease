package co.id.finease.config;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import co.id.finease.entity.Client;

import java.util.Collection;

public class ClientAuthenticationToken extends AbstractAuthenticationToken {
    private final Client client;

    public ClientAuthenticationToken(Client client, Collection<? extends GrantedAuthority> authorities, boolean isAuthetiicated) {
        super(authorities);
        this.client = client;
        setAuthenticated(isAuthetiicated);
    }

    @Override
    public Object getCredentials() {
        return null;  // No credentials like passwords for client authentication
    }

    @Override
    public Object getPrincipal() {
        return client;  // Return the client as the principal
    }

    public Client getClient() {
        return client;
    }
}
