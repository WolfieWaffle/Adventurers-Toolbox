package toolbox.common.items.tools;

import java.util.List;

import com.google.common.collect.Multimap;

import api.materials.Materials;
import com.google.common.collect.HashMultimap;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;
import toolbox.Toolbox;
import toolbox.common.Config;

public class ItemATDagger extends ItemSword implements IBladeTool, IHandleTool, IAdornedTool {

	private String name = "dagger";
	public static final String DAMAGE_TAG = "Damage";
	
	public ItemATDagger() {
		super(ToolMaterial.WOOD);

		setRegistryName(name);
		setUnlocalizedName(Toolbox.MODID + "." + name);
		this.maxStackSize = 1;
		setCreativeTab(Toolbox.weaponsTab);
		this.setMaxDamage(0);
	}

	public int getDurability(ItemStack stack) {
		return (int) (IBladeTool.getBladeMat(stack).getDurability()
				* IHandleTool.getHandleMat(stack).getDurabilityMod()
				* IAdornedTool.getAdornmentMat(stack).getDurabilityMod() * 0.75F);
	}

	public float getEfficiencyMod(ItemStack stack) {
		return ((IAdornedTool.getAdornmentMat(stack).getEfficiencyMod()
				* IHandleTool.getHandleMat(stack).getEfficiencyMod())
				- 1F) * 0.25F;
	}

	public float getAttackDamage(ItemStack stack) {
		return IBladeTool.getBladeMat(stack).getAttackDamage()
				+ IAdornedTool.getAdornmentMat(stack).getAttackDamageMod();
	}

	public int getEnchantability(ItemStack stack) {
		return (int) (IBladeTool.getBladeMat(stack).getEnchantability()
				* IAdornedTool.getAdornmentMat(stack).getEnchantabilityMod()
				* IHandleTool.getHandleMat(stack).getEnchantabilityMod());
	}

	public ItemStack getRepairItem(ItemStack stack) {
		return IBladeTool.getBladeMat(stack).getRepairItem();
	}

	@Override
	public float getDestroySpeed(ItemStack stack, IBlockState state) {
		Block block = state.getBlock();

		if (block == Blocks.WEB) {
			return 15.0F;
		} else {
			Material material = state.getMaterial();
			return material != Material.PLANTS && material != Material.VINE && material != Material.CORAL
					&& material != Material.LEAVES && material != Material.GOURD ? 1.0F : 1.5F;
		}
	}

	@Override
	public boolean canHarvestBlock(IBlockState blockIn) {
		return blockIn.getBlock() == Blocks.WEB;
	}

	@Override
	public int getItemEnchantability(ItemStack stack) {
		return getEnchantability(stack);
	}

	@Override
	public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
		ItemStack mat = getRepairItem(toRepair);
		if (!mat.isEmpty() && net.minecraftforge.oredict.OreDictionary.itemMatches(mat, repair, false)) {
			return true;
		}
		if (toRepair.getItem() == this) {
			if (OreDictionary.containsMatch(false, OreDictionary.getOres(IBladeTool.getBladeMat(toRepair).getCraftingItem()), repair)) return true;
		}
		return super.getIsRepairable(toRepair, repair);
	}

	@Override
	public Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot equipmentSlot,
			ItemStack stack) {
		Multimap<String, AttributeModifier> multimap = HashMultimap.<String, AttributeModifier>create();

		if (equipmentSlot == EntityEquipmentSlot.MAINHAND) {
			multimap.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(ATTACK_DAMAGE_MODIFIER,
					"Weapon modifier", (double) (3.0F + this.getAttackDamage(stack)) * 0.66667F, 0));
			multimap.put(SharedMonsterAttributes.ATTACK_SPEED.getName(), new AttributeModifier(ATTACK_SPEED_MODIFIER,
					"Weapon modifier", (double) (-1.8000000953674316D + getEfficiencyMod(stack)), 0));
		}

		return multimap;
	}

	@Override
	public double getDurabilityForDisplay(ItemStack stack) {
		return (double) getDamage(stack) / (double) getDurability(stack);
	}

	@Override
	public int getMaxDamage(ItemStack stack) {
		return getDurability(stack);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> subItems) {
		if(!Config.DISABLED_TOOLS.contains("dagger")) {
			ItemStack stack1 = new ItemStack(this);
			NBTTagCompound tag = new NBTTagCompound();
			tag.setString(BLADE_TAG, Materials.randomHead().getName());
			tag.setString(HANDLE_TAG, Materials.randomHandle().getName());
			tag.setString(ADORNMENT_TAG, Materials.randomAdornment().getName());
			stack1.setTagCompound(tag);
			if (isInCreativeTab(tab)) {
				subItems.add(stack1);
			}
		}
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		return I18n.translateToLocal(IBladeTool.getBladeMat(stack).getName() + ".name") + " "
				+ super.getItemStackDisplayName(stack);
	}

	@Override
	public boolean canApplyAtEnchantingTable(ItemStack stack, net.minecraft.enchantment.Enchantment enchantment) {
		return enchantment.type.canEnchantItem(stack.getItem()) || enchantment.type == EnumEnchantmentType.WEAPON;
	}

	@Override
	public boolean isEnchantable(ItemStack stack) {
		return true;
	}
	
	@Override
	public boolean isDamageable() {
		return true;
	}

	public void initModel() {
		ModelLoader.setCustomModelResourceLocation(this, 0, new ModelResourceLocation(getRegistryName().toString()));
	}

}
