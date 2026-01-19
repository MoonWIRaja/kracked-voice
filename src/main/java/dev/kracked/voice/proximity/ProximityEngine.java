package dev.kracked.voice.proximity;

/**
 * Calculates audio volume based on player distance
 */
public class ProximityEngine {
    
    private final double maxDistance;
    
    public ProximityEngine(double maxDistance) {
        this.maxDistance = maxDistance;
    }
    
    /**
     * Calculate volume multiplier based on distance
     * @param listenerX Listener X position
     * @param listenerY Listener Y position
     * @param listenerZ Listener Z position
     * @param speakerX Speaker X position
     * @param speakerY Speaker Y position
     * @param speakerZ Speaker Z position
     * @return Volume from 0.0 (silent) to 1.0 (full volume)
     */
    public double calculateVolume(double listenerX, double listenerY, double listenerZ,
                                   double speakerX, double speakerY, double speakerZ) {
        double distance = calculateDistance(listenerX, listenerY, listenerZ, speakerX, speakerY, speakerZ);
        return calculateVolumeFromDistance(distance);
    }
    
    /**
     * Calculate 3D distance between two points
     */
    public double calculateDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    /**
     * Calculate volume from distance using linear falloff
     */
    public double calculateVolumeFromDistance(double distance) {
        if (distance >= maxDistance) {
            return 0.0;
        }
        if (distance <= 0) {
            return 1.0;
        }
        
        // Linear falloff: volume = 1 - (distance / maxDistance)
        return 1.0 - (distance / maxDistance);
    }
    
    /**
     * Calculate volume with exponential falloff (alternative)
     */
    public double calculateVolumeExponential(double distance) {
        if (distance >= maxDistance) {
            return 0.0;
        }
        if (distance <= 0) {
            return 1.0;
        }
        
        // Exponential falloff: more natural sound dropoff
        double normalized = distance / maxDistance;
        return Math.pow(1.0 - normalized, 2);
    }
    
    /**
     * Check if two players are in voice range
     */
    public boolean isInRange(double x1, double y1, double z1, double x2, double y2, double z2) {
        return calculateDistance(x1, y1, z1, x2, y2, z2) < maxDistance;
    }
    
    /**
     * Apply volume to audio samples
     */
    public short[] applyVolume(short[] samples, double volume) {
        if (volume >= 1.0) {
            return samples;
        }
        if (volume <= 0.0) {
            return new short[samples.length];
        }
        
        short[] result = new short[samples.length];
        for (int i = 0; i < samples.length; i++) {
            result[i] = (short) (samples[i] * volume);
        }
        return result;
    }
    
    public double getMaxDistance() {
        return maxDistance;
    }
}
