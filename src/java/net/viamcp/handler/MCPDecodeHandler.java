//Deobfuscated with https://github.com/SimplyProgrammer/Minecraft-Deobfuscator3000 using mappings "C:\Users\Administrator\Downloads\Minecraft1.12.2 Mappings"!

//Decompiled by Procyon!

package net.viamcp.handler;

import io.netty.handler.codec.*;
import io.netty.buffer.*;
import com.viaversion.viaversion.api.connection.*;
import io.netty.channel.*;
import java.util.*;
import java.util.function.*;
import java.lang.reflect.*;
import com.viaversion.viaversion.exception.*;
import com.viaversion.viaversion.util.*;

@ChannelHandler.Sharable
public class MCPDecodeHandler extends MessageToMessageDecoder<ByteBuf>
{
    private final UserConnection info;
    private boolean handledCompression;
    private boolean skipDoubleTransform;

    public MCPDecodeHandler(UserConnection info) {
        this.info = info;
    }

    public UserConnection getInfo() {
        return info;
    }

    // https://github.com/ViaVersion/ViaVersion/blob/master/velocity/src/main/java/us/myles/ViaVersion/velocity/handlers/VelocityDecodeHandler.java
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf bytebuf, List<Object> out) throws Exception
    {
        if (skipDoubleTransform)
        {
            skipDoubleTransform = false;
            out.add(bytebuf.retain());
            return;
        }

        if (!info.checkIncomingPacket())
        {
            throw CancelDecoderException.generate(null);
        }

        if (!info.shouldTransformPacket())
        {
            out.add(bytebuf.retain());
            return;
        }

        ByteBuf transformedBuf = ctx.alloc().buffer().writeBytes(bytebuf);

        try
        {
            boolean needsCompress = handleCompressionOrder(ctx, transformedBuf);

            info.transformIncoming(transformedBuf, CancelDecoderException::generate);

            if (needsCompress)
            {
                CommonTransformer.compress(ctx, transformedBuf);
                skipDoubleTransform = true;
            }

            out.add(transformedBuf.retain());
        }
        finally
        {
            transformedBuf.release();
        }
    }

    private boolean handleCompressionOrder(ChannelHandlerContext ctx, ByteBuf buf) throws InvocationTargetException
    {
        if (handledCompression)
        {
            return false;
        }

        int decoderIndex = ctx.pipeline().names().indexOf("decompress");

        if (decoderIndex == -1)
        {
            return false;
        }

        handledCompression = true;

        if (decoderIndex > ctx.pipeline().names().indexOf("via-decoder"))
        {
            // Need to decompress this packet due to bad order
            CommonTransformer.decompress(ctx, buf);
            ChannelHandler encoder = ctx.pipeline().get("via-encoder");
            ChannelHandler decoder = ctx.pipeline().get("via-decoder");
            ctx.pipeline().remove(encoder);
            ctx.pipeline().remove(decoder);
            ctx.pipeline().addAfter("compress", "via-encoder", encoder);
            ctx.pipeline().addAfter("decompress", "via-decoder", decoder);
            return true;
        }

        return false;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        if (PipelineUtil.containsCause(cause, CancelCodecException.class))
        {
            return;
        }
        super.exceptionCaught(ctx, cause);
    }
}
