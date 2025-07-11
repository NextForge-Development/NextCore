package gg.nextforge.textblockitemdisplay.impl;

import gg.nextforge.textblockitemdisplay.Hologram;
import org.bukkit.Location;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Base implementation for holograms providing thread-safe updates.
 */
public abstract class AbstractHologram implements Hologram {
    protected final String name;
    protected Location location;
    protected boolean isTransient;
    private boolean spawned;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    protected AbstractHologram(String name, Location location) {
        this.name = name;
        this.location = location.clone();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Location getLocation() {
        lock.readLock().lock();
        try {
            return location.clone();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void setLocation(Location location) {
        lock.writeLock().lock();
        try {
            this.location = location.clone();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void spawn() {
        lock.writeLock().lock();
        try {
            this.spawned = true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void despawn() {
        lock.writeLock().lock();
        try {
            this.spawned = false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean isSpawned() {
        lock.readLock().lock();
        try {
            return spawned;
        } finally {
            lock.readLock().unlock();
        }
    }
}
