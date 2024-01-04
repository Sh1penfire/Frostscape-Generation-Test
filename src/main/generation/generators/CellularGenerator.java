package main.generation.generators;

import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Point2;
import arc.math.geom.Vec2;
import arc.struct.Seq;
import arc.util.Log;
import main.generation.GenerationSettings;
import main.generation.MapGenerator;
import mindustry.Vars;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Bullet;
import mindustry.gen.UnitEntity;
import mindustry.world.Block;

import main.generation.generators.CellularGenerator.*;

public class CellularGenerator extends MapGenerator<CellularSettings> {

    public static Point2[] neighbours = new Point2[24];
    public static Point2[] scaledNeighbours;
    {
        for (int i = 0; i < 25; i++) {
            int index = i;
            if(i == 12) continue;
            //shift all numbers 1 back after the middle index
            if(i > 12) index--;
            neighbours[index] = new Point2((i % 5 - 2), Mathf.floor(((float) i)/5) - 2);
        }
    }

    public static class CellularSettings extends GenerationSettings{
        public int startX;
        public int startY;
        public int endX;
        public int endY;
        //Scale of the noise. Important! Bigger scale means upsized output map. Only supports upscaling
        public int scl;
        public int survivalScore;
        public float chance;
        public int iterations;
        public Block air;
        public Block solid;

        public CellularSettings(int startX, int startY, int endX, int endY, int survivalScore, float chance, int iterations, int scl, Block air, Block solid){
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.survivalScore = survivalScore;
            this.chance = chance;
            this.iterations = iterations;
            this.scl = scl;
            this.air = air;
            this.solid = solid;
        }
    }

    @Override
    public void start(CellularSettings settings) {

        int width = Math.abs(settings.endX-settings.startX);
        int height = Math.abs(settings.endY - settings.startY);
        boolean[][] life = new boolean[height][width], last = new boolean[height][width];
        int[][] scores = new int[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                last[y][x] = life[y][x] = Mathf.chance(settings.chance);
            }
        }

        for (int i = 0; i < settings.iterations; i++) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int score = 0;

                    //Check all nearby points in a 3x3 square
                    for (Point2 nearby: neighbours) {
                        Point2 neighbour = nearby.cpy().add(x,y);
                        if(neighbour.x >= width || neighbour.x < 0 || neighbour.y >= height || neighbour.y < 0) continue;
                        if(last[neighbour.y][neighbour.x]) score++;
                    }

                    last[y][x] = life[y][x];
                    life[y][x] = (score >= settings.survivalScore);
                }
            }
        }


        if(settings.scl == 1){
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    Vars.world.tile(settings.startX + x, settings.startY + y).setBlock(life[y][x] ? settings.solid : settings.air);
                }
            }
        }
        else {
            int size = Mathf.pow((settings.scl - 1) * 2 + 1, 2);
            int len = settings.scl + 1;
            Log.info(size);
            int excluded = (size - 1)/2;
            scaledNeighbours = new Point2[size-1];
            for (int i = 0; i < size; i++) {
                int index = i;
                if(i == excluded) continue;
                //shift all numbers 1 back after the middle index
                if(i > excluded) index--;
                scaledNeighbours[index] = new Point2((i % len - settings.scl) + settings.scl/2, Mathf.floor(((float) i)/len) - settings.scl/2);
            }

            Log.info(Seq.with(scaledNeighbours));

            for (int y = 0; y < height * settings.scl; y++) {
                for (int x = 0; x < width * settings.scl; x++) {

                    int mapX = Mathf.floor((float)y/settings.scl);
                    int mapY = Mathf.floor((float)y/settings.scl);

                    int score = life[y][x] ? 1 : 0;

                    for (Point2 nearby: scaledNeighbours) {
                        Point2 neighbour = nearby.cpy().add(mapX,mapY);
                        if(neighbour.x >= width || neighbour.x < 0 || neighbour.y >= height || neighbour.y < 0) continue;
                        if(last[neighbour.y][neighbour.x]) score++;
                    }

                    boolean solid = score >= excluded;
                    Vars.world.tile(settings.startX + x, settings.startY + y).setBlock(solid ? settings.solid : settings.air);
                }
            }
        }
    }
}
