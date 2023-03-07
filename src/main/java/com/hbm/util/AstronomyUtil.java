package com.hbm.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class AstronomyUtil
{
	public static final float AUToKm = 149598000F;
	public static final long offset = 1000000;
	public static final float day = 24000;
	//public static final long offset = 0;
	//https://www.desmos.com/calculator/h2v3nfaopa
	public static final float KerbolRadius=261600;
	//public static final float GillyRadius=13F;
	public static final float MunRadius=200F;
	public static final float MinmusRadius=60F;
	/*public static final float IkeRadius=13F;
	public static final float LaytheRadius=13F;
	public static final float VallRadius=13F;
	public static final float TyloRadius=13F;
	public static final float BopRadius=13F;
	public static final float PolRadius=13F;*/
	
    public static final float MohoRadius=250;
    public static final float EveRadius=700;
    public static final float KerbinRadius=600F;    
    public static final float DunaRadius=320;
    public static final float DresRadius=138;
    public static final float JoolRadius=6000;
    //public static final float SarnusRadius=60268;
    //public static final float UrlumRadius=25559;
    //public static final float NeidonRadius=24766;
    //public static final float PlockRadius=1184;
	
	public static final float MohoAU=5263138.304F/AUToKm;
    public static final float EveAU=9832684.544F/AUToKm;
    public static final float KerbinAU=13599840.256F/AUToKm;    
    public static final float DunaAU=20726155.264F/AUToKm;
    public static final float DresAU=40839348.203F/AUToKm;
    public static final float JoolAU=68773560.320F/AUToKm;
    //public static final float SarnusAU=9.54F;
    //public static final float UrlumAU=19.19F;
    //public static final float NeidonAU=30.1F;
    //public static final float PlockAU=39.5F;
    
    public static final float MohoP=102.58F;
    public static final float EveP=261.94F;
    public static final float KerbinP=365.25F;    
    public static final float DunaP=801.6389F;
    public static final float DresP=2217.27F;
    public static final float JoolP=4845.4367F;
   // public static final float SarnusP=10756;
   // public static final float UrlumP=30687;
   // public static final float NeidonP=60190;
   // public static final float PlockP=90553;
    
    public static final float MunP=6.43F;
    public static final float MinmusP=49.88F;
    
    public static final float MunKerbinKm=12000;
    public static final float MinmusKerbinKm=47000;
    //public static final float IkeDunaKm=9377.2F-DunaRadius;
    
    public static ResourceLocation mohoTexture = new ResourceLocation("hbm:textures/misc/moho.png");
    public static ResourceLocation eveTexture = new ResourceLocation("hbm:textures/misc/eve.png");
    public static ResourceLocation kerbinTexture = new ResourceLocation("hbm:textures/misc/kerbin.png");
    public static ResourceLocation dunaTexture = new ResourceLocation("hbm:textures/misc/duna.png");
    public static ResourceLocation joolTexture = new ResourceLocation("hbm:textures/misc/jool.png");
    public static ResourceLocation sarnusTexture = new ResourceLocation("hbm:textures/misc/sarnus.png");
    public static ResourceLocation urlumTexture = new ResourceLocation("hbm:textures/misc/urlum.png");
    public static ResourceLocation neidonTexture = new ResourceLocation("hbm:textures/misc/neidon.png");
    public static ResourceLocation plockTexture = new ResourceLocation("hbm:textures/misc/plock.png");
    
    /**
     * Calculates the maximum angular distance from the Sun that the planet can appear in the sky. The first planet is the one that you are running the calculation for, and the second planet is the one you're standing on.
     */
    public static double getMaxPlanetaryElongation(World world, Float planet1, Float planet2) {
    	return Math.toDegrees(Math.asin(planet1/planet2));
    }
    
    public static double getInterplanetaryDistance(World world, Float planet1AU, Float planet1Period, Float planet2AU, Float planet2Period) {
    	//System.out.println(planet1+" "+planet2);
    	Complex PlanetAAu = new Complex(planet1AU,0);
    	Complex PlanetBAu = new Complex(planet2AU,0);
    	double time = ((double)(world.getWorldTime()+offset)/day);
    	double timeFactor1 = (time/planet1Period)*Math.pow(planet1AU, (-3/2));
    	Complex PlanetImaginary = new Complex(0,1);
    	Complex PlanetATime = new Complex(timeFactor1,0);
    	Complex Pi = new Complex(Math.PI*2,0);
    	Complex PlanetAPeriodFactor = PlanetImaginary.times(PlanetATime.times(Pi));
    	Complex PlanetAExp = PlanetAPeriodFactor.exp();
    	Complex PlanetAFinal = PlanetAExp.times(PlanetAAu);
    	
    	double timeFactor2 = (time/planet2Period)*Math.pow(planet2AU, (-3/2));
    	Complex PlanetBTime = new Complex(timeFactor2,0);
    	Complex PlanetBPeriodFactor = PlanetImaginary.times(PlanetBTime.times(Pi));
    	Complex PlanetBExp = PlanetBPeriodFactor.exp();
    	Complex PlanetBFinal = PlanetBExp.times(PlanetBAu);
    	
    	Complex FinalTotal = PlanetAFinal.minus(PlanetBFinal);
    	
    	double distance = FinalTotal.mod();
    	return distance;
    	//}
    }
    
    /**
     * Calculates the synodic period between two planets. The first planet is the one closest to the sun, and the second planet is the more distant one.
     */
    public static float calculateSynodicPeriod(Float planet1Period, Float planet2Period)
    {
    	float subperiod = (1/planet1Period)-(1/planet2Period);
		//float period = 24000*(1/(1/planet1Period)-(1/planet2Period));
    	float period = day*(1/subperiod);
    	return period;
    }
    /**
     * Calculates the visual angle between two planets. The first planet is the one closest to the sun, and the second planet is the more distant one.
     */
    public static float calculatePlanetAngle(long par1, float par3, float planet1Period, float planet2Period)
    {
    	float synodic = calculateSynodicPeriod(planet1Period, planet2Period);
        par1 = Minecraft.getMinecraft().theWorld.getWorldTime()+offset;
        int j = (int) (par1 % synodic);
        float f1 = (j + par3) / synodic - 0.25F;

        if (f1 < 0.0F)
        {
            ++f1;
        }

        if (f1 > 1.0F)
        {
            --f1;
        }

        float f2 = f1;
        f1 = 0.5F - MathHelper.cos(f1 * 3.1415927F) / 2.0F;
        return f2 + (f1 - f2) / 3.0F;
    }
    public static float calculateStarAngle(long par1, float par3, float planetPeriod)
    {
        par1 = Minecraft.getMinecraft().theWorld.getWorldTime();
        int j = (int) (par1 % (day*planetPeriod));
        float f1 = (j + par3) / (day*planetPeriod) - 0.25F;

        if (f1 < 0.0F)
        {
            ++f1;
        }

        if (f1 > 1.0F)
        {
            --f1;
        }

        float f2 = f1;
        f1 = 0.5F - MathHelper.cos(f1 * 3.1415927F) / 2.0F;
        return f2 + (f1 - f2) / 3.0F;
    }
    public static float calculateMoonAngle(long par1, float par3, float time)
    {    	
        par1 = Minecraft.getMinecraft().theWorld.getWorldTime()+offset;
        int j = (int) (par1 % time);
        float f1 = (j + par3) / time - 0.25F;

        if (f1 < 0.0F)
        {
            ++f1;
        }

        if (f1 > 1.0F)
        {
            --f1;
        }

        float f2 = f1;
        f1 = 0.5F - MathHelper.cos(f1 * 3.1415927F) / 2.0F;
        return f2 + (f1 - f2) / 3.0F;
    }
}
