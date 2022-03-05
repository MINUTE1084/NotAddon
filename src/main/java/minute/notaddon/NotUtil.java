package minute.notaddon;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class NotUtil {
    public static Vector rotateVector(Vector vec, Vector axis, double angle) {
        double x = vec.getX();
        double y = vec.getY();
        double z = vec.getZ();

        double u = axis.getX();
        double v = axis.getY();
        double w = axis.getZ();

        double data = (u * x) + (v * y) + (w * z);

        double xPrime = u * data * (1d - Math.cos(angle))
                + x * Math.cos(angle)
                + (-w * y + v * z) * Math.sin(angle);

        double yPrime = v * data * (1d - Math.cos(angle))
                + y * Math.cos(angle)
                + (w * x - u * z) * Math.sin(angle);

        double zPrime = w * data * (1d - Math.cos(angle))
                + z * Math.cos(angle)
                + (-v * x + u * y) * Math.sin(angle);;

        return new Vector(xPrime, yPrime, zPrime);
    }

    public static void drawLine(Location point1, Location point2, Particle[] particles, int count, double offset, double speed, Object[] options) {
        if (point1.getWorld() == null || point2.getWorld() == null || !point1.getWorld().equals(point2.getWorld())) return;
        World world = point1.getWorld();

        double distance = point1.distance(point2);
        Vector p1 = point1.toVector();
        Vector p2 = point2.toVector();
        Vector vector = p2.clone().subtract(p1).normalize();

        for (float i = 0; i <= distance; i += (offset * 2)) {
            for (int j = 0; j < particles.length; j++) world.spawnParticle(particles[j], new Location(world, p1.getX(), p1.getY(), p1.getZ()), count, offset, offset, offset, speed, options[j]);
            p1.add(vector);
        }
    }

    public static void drawPersonalLine(Player player, Location point1, Location point2, Particle[] particles, int count, double offset, double speed, Object[] options) {
        if (point1.getWorld() == null || point2.getWorld() == null || !point1.getWorld().equals(point2.getWorld()) || !point1.getWorld().equals(player.getWorld())) return;

        double distance = point1.distance(point2);
        Vector p1 = point1.toVector();
        Vector p2 = point2.toVector();
        Vector vector = p2.clone().subtract(p1).normalize();

        for (float i = 0; i <= distance; i += (offset * 2)) {
            for (int j = 0; j < particles.length; j++) player.spawnParticle(particles[j], new Location(point1.getWorld(), p1.getX(), p1.getY(), p1.getZ()), count, offset, offset, offset, speed, options[j]);
            p1.add(vector);
        }
    }

    public static void regenHealth(Player player, double amount) {
        double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
        double targetHealth = player.getHealth() + amount;
        if (targetHealth > maxHealth) targetHealth = maxHealth;

        player.setHealth(targetHealth);
    }
}
