package dev.thomazz.pledge.spigot.network;

import dev.thomazz.pledge.spigot.api.event.ActivateHandlerEvent;
import dev.thomazz.pledge.spigot.PlayerHandlerImpl;
import dev.thomazz.pledge.spigot.PledgeSpigot;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;

@RequiredArgsConstructor
public class PacketFrameOutboundHeadHandler extends ChannelOutboundHandlerAdapter {
	public static final String HANDLER_NAME = "pledge_frame_outbound_head";

	private final PledgeSpigot pledge;
	private final PlayerHandlerImpl playerHandler;
	private final PacketFrameOutboundTailHandler tailHandler;

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		// Login event handling
		if (this.pledge.getPacketProvider().isLogin(msg)) {
			// Handler activation event on login
			Bukkit.getPluginManager().callEvent(new ActivateHandlerEvent(this.playerHandler.getPlayer(), ctx.channel()));
			super.write(ctx, msg, promise);

			// Send validation transaction
			Object validation = this.pledge.getPacketProvider().buildPacket(this.playerHandler.getRangeStart());
			super.write(ctx, validation, ctx.newPromise());

			// Start handler after validation has been sent
			this.start();
			return;
		}

		// Normal behaviour before handler starts
		if (!this.playerHandler.isActive()) {
			super.write(ctx, msg, promise);
			return;
		}

		// Communicate to queue handler that some packets should be discarded or not since we queue them here
		if (this.pledge.getPacketProvider().isDisconnect(msg) || this.pledge.getPacketProvider().isKeepAlive(msg)) {
			this.tailHandler.setQueue(false);
			try {
				super.write(ctx, msg, promise);
				super.flush(ctx);
			} finally {
				this.tailHandler.setQueue(true);
			}
		} else {
			super.write(ctx, msg, promise);
		}
	}

	public void start() {
		this.playerHandler.setActive(true);
	}
}
