package com.p1ut0nium.roughmobsrevamped.client.model;

import com.p1ut0nium.roughmobsrevamped.entities.EntityHostileBat;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/*
 * CHANGED: the 1.12.2 class was a copy of vanilla ModelBat. This is a copy of the 1.20.1 vanilla BatModel,
 * baked from the vanilla bat layer, with the hanging check reading EntityHostileBat.
 */
@OnlyIn(Dist.CLIENT)
public class ModelHostileBat extends HierarchicalModel<EntityHostileBat> {
	private final ModelPart root;
	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart rightWing;
	private final ModelPart leftWing;
	private final ModelPart rightWingTip;
	private final ModelPart leftWingTip;

	public ModelHostileBat(ModelPart root) {
		this.root = root;
		this.head = root.getChild("head");
		this.body = root.getChild("body");
		this.rightWing = this.body.getChild("right_wing");
		this.rightWingTip = this.rightWing.getChild("right_wing_tip");
		this.leftWing = this.body.getChild("left_wing");
		this.leftWingTip = this.leftWing.getChild("left_wing_tip");
	}

	@Override
	public ModelPart root() {
		return this.root;
	}

	@Override
	public void setupAnim(EntityHostileBat entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		if (entity.getIsBatHanging()) {
			this.head.xRot = headPitch * ((float)Math.PI / 180F);
			this.head.yRot = (float)Math.PI - netHeadYaw * ((float)Math.PI / 180F);
			this.head.zRot = (float)Math.PI;
			this.head.setPos(0.0F, -2.0F, 0.0F);
			this.rightWing.setPos(-3.0F, 0.0F, 3.0F);
			this.leftWing.setPos(3.0F, 0.0F, 3.0F);
			this.body.xRot = (float)Math.PI;
			this.rightWing.xRot = -0.15707964F;
			this.rightWing.yRot = -1.2566371F;
			this.rightWingTip.yRot = -1.7278761F;
			this.leftWing.xRot = this.rightWing.xRot;
			this.leftWing.yRot = -this.rightWing.yRot;
			this.leftWingTip.yRot = -this.rightWingTip.yRot;
		} else {
			this.head.xRot = headPitch * ((float)Math.PI / 180F);
			this.head.yRot = netHeadYaw * ((float)Math.PI / 180F);
			this.head.zRot = 0.0F;
			this.head.setPos(0.0F, 0.0F, 0.0F);
			this.rightWing.setPos(0.0F, 0.0F, 0.0F);
			this.leftWing.setPos(0.0F, 0.0F, 0.0F);
			this.body.xRot = ((float)Math.PI / 4F) + Mth.cos(ageInTicks * 0.1F) * 0.15F;
			this.body.yRot = 0.0F;
			this.rightWing.yRot = Mth.cos(ageInTicks * 74.48451F * ((float)Math.PI / 180F)) * (float)Math.PI * 0.25F;
			this.leftWing.yRot = -this.rightWing.yRot;
			this.rightWingTip.yRot = this.rightWing.yRot * 0.5F;
			this.leftWingTip.yRot = -this.rightWing.yRot * 0.5F;
		}
	}
}
