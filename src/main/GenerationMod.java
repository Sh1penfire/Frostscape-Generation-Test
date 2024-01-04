package main;

import arc.*;
import arc.struct.Seq;
import arc.util.*;
import main.generation.generators.CellularGenerator;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.entities.Damage;
import mindustry.game.EventType.*;
import mindustry.mod.*;
import mindustry.ui.dialogs.*;
import mindustry.world.blocks.payloads.PayloadBlock;
import mindustry.world.blocks.units.UnitFactory;
import rhino.ImporterTopLevel;
import rhino.NativeJavaPackage;
import rhino.Scriptable;

public class GenerationMod extends Mod{

    public static NativeJavaPackage p = null;
    public static CellularGenerator generator;
    public static CellularGenerator.CellularSettings settings;

    public GenerationMod(){
    }

    @Override
    public void init() {
        super.init();
        Seq<String> packages = Seq.with(
                "main",
                "main.generation",
                "main.generation.generators"
        );

        ImporterTopLevel scope = (ImporterTopLevel) Vars.mods.getScripts().scope;

        packages.each(name -> {
            p = new NativeJavaPackage(name, Vars.mods.mainLoader());

            p.setParentScope(scope);

            scope.importPackage(p);
        });
    }

    @Override
    public void loadContent(){
        generator = new CellularGenerator();
        settings = new CellularGenerator.CellularSettings(0,0,250,250,16,0.75f,5, 2, Blocks.air,Blocks.stoneWall);
    }

    public static void generate(){
        Time.run(1,() -> generator.start(settings));
        /*
        Copy this into generate after calling
        [{class:clear,seed:778591109,target:stone-wall,replace:ferric-stone},{class:blend,seed:902394408,radius:1.98,block:stone,floor:stone},{class:blend,seed:122481267,radius:1.98,block:stone,floor:stone},{class:clear,seed:304650618},{class:clear,seed:371754260,target:ferric-stone,replace:stone},{class:median,seed:995588116,radius:4.05,percentile:0.375},{class:distort,seed:962299413,scl:13.93,mag:14.4275},{class:terrain,seed:390699992,scl:39.92,octaves:3.015,circleScl:0.945}]
         */
    }
}
