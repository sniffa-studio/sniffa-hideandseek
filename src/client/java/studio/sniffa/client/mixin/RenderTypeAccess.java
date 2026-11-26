package studio.sniffa.client.mixin;

import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderType.class)
public interface RenderTypeAccess {

    @Invoker("create")
    static RenderType hideandseek$create(String name, RenderSetup setup) {
        throw new AssertionError();
    }
}
