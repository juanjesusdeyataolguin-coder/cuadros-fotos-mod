package com.fraxxz.fotos;

import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class PhotoFrameRenderer implements BlockEntityRenderer<PhotoFrameBlockEntity> {
    public PhotoFrameRenderer(BlockEntityRendererFactory.Context ctx) { }

    @Override
    public void render(PhotoFrameBlockEntity be, float tickDelta, MatrixStack m,
                       VertexConsumerProvider vcp, int light, int overlay) {
        String id = be.getPhotoId();
        if (id.isEmpty()) return;
        PhotoCache.Entry e = PhotoCache.get(id);
        if (e == null) return;
        Direction f = be.getCachedState().get(PhotoFrameBlock.FACING);

        float hw = 0.4375f, hh = 0.4375f;
        if (e.w >= e.h) hh = hw * e.h / e.w; else hw = hh * e.w / e.h;

        m.push();
        m.translate(0.5, 0.5, 0.5);
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - f.asRotation()));
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(e.tex));
        MatrixStack.Entry en = m.peek();
        Matrix4f pm = en.getPositionMatrix();
        Matrix3f nm = en.getNormalMatrix();
        float z = 0.373f;
        int lm = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        v(vc, pm, nm, hw, hh, z, 0, 0, lm);
        v(vc, pm, nm, hw, -hh, z, 0, 1, lm);
        v(vc, pm, nm, -hw, -hh, z, 1, 1, lm);
        v(vc, pm, nm, -hw, hh, z, 1, 0, lm);
        m.pop();
    }

    private static void v(VertexConsumer vc, Matrix4f pm, Matrix3f nm, float x, float y, float z, float u, float v, int light) {
        vc.vertex(pm, x, y, z).color(255, 255, 255, 255).texture(u, v)
            .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(nm, 0, 0, -1).next();
    }
}
