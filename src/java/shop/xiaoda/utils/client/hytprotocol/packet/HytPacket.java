//Deobfuscated with https://github.com/SimplyProgrammer/Minecraft-Deobfuscator3000 using mappings "C:\Users\Administrator\Downloads\Minecraft1.12.2 Mappings"!

//Decompiled by Procyon!

package shop.xiaoda.utils.client.hytprotocol.packet;

import net.minecraft.client.*;
import io.netty.buffer.*;
import java.io.*;

public interface HytPacket
{
    public static final Minecraft mc = Minecraft.getMinecraft();
    
    String getChannel();
    
    void process(final ByteBuf p0) throws IOException;
}