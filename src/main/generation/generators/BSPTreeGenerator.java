package main.generation.generators;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Interp;
import arc.math.Mathf;
import arc.math.geom.Rect;
import arc.math.geom.Vec2;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Time;
import arc.util.Tmp;
import main.generation.GenerationSettings;
import main.generation.MapGenerator;
import mindustry.Vars;
import mindustry.entities.Effect;
import mindustry.game.EventType;
import mindustry.graphics.Layer;
import mindustry.world.Block;

import static main.generation.generators.BSPTreeGenerator.*;

public class BSPTreeGenerator extends MapGenerator<BSPTreeSettings> {

    public static int maxSize = 20, minSize = 6;
    public static Seq<Leaf> leaves = new Seq<>(),

    /** Only stores leaves which haven't branched. Used for the adjacency graph */
    finalLeaves = new Seq<>();

    public static Leaf root;


    public static class Leaf {

        /**
         * Position at bottom left corner. Tile-aligned
         */
        public int x;
        public int y;
        public int width;
        public int height;

        /** Children of this leaf if this isn't the end of a branch */
        public Leaf
                leftChild,
                rightChild;

        /** Adjacent neighbours if this is at the end of a branch */
        public Seq<Leaf> neighbours;

        /** Depth into the BSP tree */
        public int depth;

        /** How the leaf was split, if it was */
        public SplitState split;

        public Rect room;

        //Pointers to other leaves
        public Seq<Vec2> hallways;

        public Leaf(int x, int y, int width, int height, int depth) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.depth = depth;
            split = SplitState.before;
            room = new Rect(x,y,width,height);
            neighbours = new Seq<>();
        }

        /**
         * Returns true if split was successful
         */
        public boolean split() {
            if (leftChild != null || rightChild != null) return false;

            boolean splitH = Mathf.randomBoolean();

            // determine direction of split
            // if the width is 2x larger than height, we split vertically
            // if the height is 2x larger than the width, we split horizontally
            // otherwise we split randomly
            if (width > height && width / height >= 2)
                splitH = false;
            else if (height > width && height / width >= 2)
                splitH = true;

            int max = (splitH ? height : width) - minSize; // determine the maximum height or width
            if (max <= minSize)
                return false; // the area is too small to split any more

            split = splitH ? SplitState.horizontal : SplitState.vertical;

            int split = Mathf.random(minSize, max); // determine where we're going to split
            // create our left and right children based on the direction of the split
            if (splitH) {
                leftChild = new Leaf(x, y, width, split, depth + 1);
                rightChild = new Leaf(x, y + split, width, height - split, depth + 1);
            } else {
                leftChild = new Leaf(x, y, split, height, depth + 1);
                rightChild = new Leaf(x + split, y, width - split, height, depth + 1);
            }

            return true;
        }
    }

    @Override
    public void start(BSPTreeSettings settings) {
        maxSize = settings.maxSize;
        minSize = settings.minSize;
        leaves.clear();
        finalLeaves.clear();

        root = new Leaf(settings.startX, settings.startY, settings.endX - settings.startX, settings.endY - settings.startY, 0);

        leaves.add(root);

        //Find if we split last time we itterated through all the leaves. If not, then we end!
        boolean hasSplit = true;

        while (hasSplit){
            hasSplit = false;
            for (Leaf leaf: leaves) {
                //detect if a leaf can't be split anymore...
                if(leaf.split()) // attempt to split the Leaf!
                {
                    // if we did split, push the child leafs to the Vector so we can loop into them next
                    leaves.add(leaf.leftChild);
                    leaves.add(leaf.rightChild);
                    hasSplit = true;
                }
            }
        }
        leaves.each(leaf -> {
            if(leaf.split == SplitState.before) finalLeaves.add(leaf);
        });

        //Setup adjacencies for each leaf
        for (int i = 0; i < finalLeaves.size; i++) {
            Leaf leaf = finalLeaves.get(i);
            Time.run(180 * i, () -> {
                Rect filterRect = new Rect(leaf.x - 1, leaf.y - 1, leaf.width + 2, leaf.height + 2);
                setupAdjacencies(leaf, root.leftChild, filterRect);
                setupAdjacencies(leaf, root.rightChild, filterRect);
            });
        }
    }

    static Effect flashRectangle = new Effect(30, 120, e -> {
        Tmp.r1.set((Rect) e.data);
        Draw.z(Layer.overlayUI - 1);
        Draw.color(e.color);
        float scl = e.finpow() * 5;
        Tmp.r1.x = (Tmp.r1.x - scl) * Vars.tilesize;
        Tmp.r1.y = (Tmp.r1.y - scl) * Vars.tilesize;
        Tmp.r1.width = (Tmp.r1.width + 2 * scl) * Vars.tilesize;
        Tmp.r1.height = (Tmp.r1.height + 2 * scl) * Vars.tilesize;
        Lines.rect(Tmp.r1);
        Lines.stroke(e.fout() * 5);
        Tmp.r1.set((Rect) e.data);
        Tmp.r2.x = Mathf.lerp(Tmp.r1.x, Tmp.r1.x + Tmp.r1.width/2, e.fin()) * Vars.tilesize;
        Tmp.r2.y = Mathf.lerp(Tmp.r1.y, Tmp.r1.y + Tmp.r1.height/2, e.fin()) * Vars.tilesize;
        Tmp.r2.width = Tmp.r1.width * e.fout() * Vars.tilesize;
        Tmp.r2.height = Tmp.r1.height * e.fout() * Vars.tilesize;
        Tmp.r1.x *= Vars.tilesize;
        Tmp.r1.y *= Vars.tilesize;
        Tmp.r1.width *= Vars.tilesize;
        Tmp.r1.height *= Vars.tilesize;
        Draw.color(e.color);
        Fill.rect(Tmp.r2);
        Lines.stroke(Interp.pow3Out.apply(e.fin()) * 10);
        Lines.rect(Tmp.r1);
    });
    //Recursive function which traverses the tree searching for nodes at the ends of branches adjacent to the leaf based on a filter
    public static void setupAdjacencies(Leaf leaf, Leaf parent, Rect filter){
        if(!filter.overlaps(parent.room)) return;
        flashRectangle.at(parent.x * Vars.tilesize,parent.y * Vars.tilesize,0,Tmp.c1.set(Color.red).shiftHue(parent.depth * 25),parent.room);
        if(parent.split == SplitState.before) {
            leaf.neighbours.add(parent);
            Log.info(parent);
        }
        else{
            Time.run(20, () -> {
                setupAdjacencies(leaf, parent.leftChild, filter);
                setupAdjacencies(leaf, parent.rightChild, filter);
            });
        }
    }

    public static class BSPTreeSettings extends GenerationSettings {
        public int startX;
        public int startY;
        public int endX;
        public int endY;

        public int minSize;

        public int maxSize;

        public BSPTreeSettings(int startX, int startY, int endX, int endY) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            minSize = 6;
            maxSize = 20;
        }

        public BSPTreeSettings(int startX, int startY, int endX, int endY, int minSize, int maxSize) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.minSize = minSize;
            this.maxSize = maxSize;
        }
    }

    public enum SplitState{
        before("Before"),
        horizontal("Horizontal"),
        vertical("Vertical");

        final String name;

        SplitState(String name){
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
