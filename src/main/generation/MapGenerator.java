package main.generation;

public abstract class MapGenerator<T extends GenerationSettings> {
    public abstract void start(T settings);
}
