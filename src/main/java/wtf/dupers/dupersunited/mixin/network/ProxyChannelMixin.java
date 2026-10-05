package wtf.dupers.dupersunited.mixin.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.proxy.Socks5ProxyHandler;
import wtf.dupers.dupersunited.features.proxies.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.dupers.dupersunited.features.proxies.ProxyConfigManager;
import wtf.dupers.dupersunited.features.proxies.ProxyProfiles;

import java.net.InetSocketAddress;

@Mixin(targets = "net.minecraft.network.Connection$1")
public abstract class ProxyChannelMixin extends ChannelInitializer<Channel> {

    @Inject(method = "initChannel", at = @At("HEAD"))
    private void injectProxy(Channel ch, CallbackInfo ci) {
        ProxyProfiles active = ProxyConfigManager.getActiveProfile();
        if (!ProxyConfigManager.globalEnabled || active == null || active.address.isEmpty()) {
            return;
        }

        InetSocketAddress proxyAddr = new InetSocketAddress(active.getHost(), active.getPort());
        Socks5ProxyHandler proxyHandler = (active.user != null && !active.user.isEmpty())
                ? new Socks5ProxyHandler(proxyAddr, active.user, active.pass)
                : new Socks5ProxyHandler(proxyAddr);

        ch.pipeline().addFirst("proxy_handler", proxyHandler);
    }
}