package wtf.dupers.dupersunited.features.cosmetics;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.modules.render.CosmeticsModule;

public final class CosmeticsFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
    private static boolean preview;
    public CosmeticsFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) { super(context); }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, PlayerEntityRenderState state, float limbAngle, float limbDistance) {
        CosmeticsModule module=MainClient.getModule(CosmeticsModule.class);
        MinecraftClient client=MinecraftClient.getInstance();
        if(module==null||(!module.isEnabled()&&!preview)||client.player==null||state.id!=client.player.getId()||state.invisible)return;
        renderHead(module,matrices,queue,light,state.age);
        renderTail(module,matrices,queue,light,state.age);
    }

    public static void preview(Runnable action){ preview=true; try{action.run();}finally{preview=false;} }

    private void renderHead(CosmeticsModule module,MatrixStack matrices,OrderedRenderCommandQueue queue,int light,float age){
        CosmeticCatalog.Item item=module.selectedHead(); if(item.cubes().isEmpty()&&item.model().isBlank())return;
        matrices.push(); getContextModel().getRootPart().applyTransform(matrices); getContextModel().head.applyTransform(matrices);
        matrices.translate(module.headX.getValue()/16f,-.16f+module.headY.getValue()/-16f,module.headZ.getValue()/16f);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(module.headPitch.getValue()));matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(module.headYaw.getValue()));matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(module.headRoll.getValue()));
        float size=.68f*module.headSize.getValue()/100f;matrices.scale(size*module.headWidth.getValue()/100f,size*module.headStretch.getValue()/100f,size*module.headWidth.getValue()/100f);
        if(item.id().equals("minecraft-duck")){matrices.translate(0,-.5f,0);matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));matrices.scale(1,-1,1);}
        if(module.animate.getValue())matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)Math.sin(age*.08f)*1.6f));
        CosmeticRenderer.render(item,matrices,queue,light); matrices.pop();
    }

    private void renderTail(CosmeticsModule module,MatrixStack matrices,OrderedRenderCommandQueue queue,int light,float age){
        CosmeticCatalog.Item item=module.selectedTail(); if(item.cubes().isEmpty()&&item.model().isBlank())return;
        matrices.push(); getContextModel().getRootPart().applyTransform(matrices); getContextModel().body.applyTransform(matrices);
        matrices.translate(module.tailX.getValue()/16f,.38f+module.tailY.getValue()/-16f,module.tailZ.getValue()/16f);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(module.tailPitch.getValue()));matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(module.tailYaw.getValue()));matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(module.tailRoll.getValue()));
        float size=.68f*module.tailSize.getValue()/100f;matrices.scale(size*module.tailWidth.getValue()/100f,size*module.tailStretch.getValue()/100f,size*module.tailWidth.getValue()/100f);
        if(module.animate.getValue())matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)Math.sin(age*.16f)*12f));
        CosmeticRenderer.render(item,matrices,queue,light); matrices.pop();
    }
}
