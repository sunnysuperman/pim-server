package com.github.sunnysuperman.pim.client;

import io.netty.channel.ChannelHandlerContext;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sunnysuperman.commons.utils.FormatUtil;
import com.github.sunnysuperman.commons.utils.JSONUtil;
import com.github.sunnysuperman.pim.protocol.ClientID;
import com.github.sunnysuperman.pim.util.PimUtil;

public abstract class DefaultClientAuthProvider implements ClientAuthProvider {
    public static final int AUTH_FAILED = 11;
    public static final int AUTH_INTERNAL_ERROR = 12;
    private static final Logger LOG = LoggerFactory.getLogger(DefaultClientAuthProvider.class);

    public DefaultClientAuthProvider() {
        super();
    }

    @Override
    public final AuthResult auth(ChannelHandlerContext context, byte[] body) {
        AuthResult result = new AuthResult();
        result.errorCode = AUTH_FAILED;
        try {
            String s = PimUtil.wrapString(body);
            if (s == null) {
                return result;
            }
            Map<String, Object> data = JSONUtil.parseJSONObject(s);
            if (data == null) {
                return result;
            }
            String username = FormatUtil.parseString(data.get("u"));
            String password = FormatUtil.parseString(data.get("p"));
            Map<?, ?> options = (Map<?, ?>) data.get("o");
            if (username == null || password == null) {
                return result;
            }
            ClientID clientID = ClientID.wrap(username);
            if (clientID == null) {
                return result;
            }
            clientID = doBeforeAuth(clientID, password, options);
            if (clientID == null) {
                return result;
            }
            int errorCode = auth(clientID, password, options);
            result.errorCode = errorCode;
            clientID = doAfterAuth(errorCode, clientID, password, options);
            if (errorCode == AUTH_OK) {
                result.clientID = clientID;
                if (options != null) {
                    result.compressEnabled = FormatUtil.parseBoolean(options.get("c"), false);
                }
            }
        } catch (Throwable t) {
            LOG.error(null, t);
            result.errorCode = AUTH_INTERNAL_ERROR;
        }
        return result;
    }

    protected ClientID doBeforeAuth(ClientID clientID, String password, Map<?, ?> options) throws Exception {
        return clientID;
    }

    protected ClientID doAfterAuth(int errorCode, ClientID clientID, String password, Map<?, ?> options)
            throws Exception {
        return clientID;
    }

    protected abstract int auth(ClientID clientID, String password, Map<?, ?> options) throws Exception;

}
