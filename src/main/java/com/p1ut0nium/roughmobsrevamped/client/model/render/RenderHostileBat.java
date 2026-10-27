package com.p1ut0nium.roughmobsrevamped.client.model.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.p1ut0nium.roughmobsrevamped.client.model.ModelHostileBat;
import com.p1ut0nium.roughmobsrevamped.entities.EntityHostileBat;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RenderHostileBat extends MobRenderer<EntityHostileBat, ModelHostileBat>
{
	private static final ResourceLocation BAT_TEXTURES = new ResourceLocation("textures/entity/bat.png");

	public RenderHostileBat(EntityRendererProvider.Context context)
	{
		super(context, new ModelHostileBat(context.bakeLayer(ModelLayers.BAT)), 0.25F);
	}

	// Returns the location of an entity's texture.
	@Override
	public ResourceLocation getTextureLocation(EntityHostileBat entity)
	{
		return BAT_TEXTURES;
	}

	// Allows the render to do state modifications necessary before the model is rendered.
	@Override
	protected void scale(EntityHostileBat entity, PoseStack poseStack, float partialTickTime)
	{
		poseStack.scale(0.35F, 0.35F, 0.35F);
	}

	@Override
	protected void setupRotations(EntityHostileBat entity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks)
	{
		if (entity.getIsBatHanging())
		{
			poseStack.translate(0.0F, -0.1F, 0.0F);
		}
		else
		{
			poseStack.translate(0.0F, Mth.cos(ageInTicks * 0.3F) * 0.1F, 0.0F);
		}

		super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks);
	}
}
