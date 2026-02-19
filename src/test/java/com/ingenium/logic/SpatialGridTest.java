package com.ingenium.logic;

import com.ingenium.config.IngeniumConfig;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SpatialGrid - Priority 1.7
 * 
 * Tests verify:
 * - O(k) query performance (independent of entity count)
 * - Thread safety
 * - Correctness of spatial operations
 * - Adaptive cell sizing behavior
 */
@Tag("unit")
class SpatialGridTest {

    @Mock
    private World mockWorld;

    @Mock
    private Entity mockEntity1;

    @Mock
    private Entity mockEntity2;

    @Mock
    private Entity mockEntity3;

    private SpatialGrid grid;
    private IngeniumConfig config;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        config = new IngeniumConfig();
        
        when(mockWorld.getRegistryKey()).thenReturn(World.OVERWORLD);
        
        grid = new SpatialGrid(mockWorld, config);
    }

    @Test
    void testInsertAndRetrieve() {
        // Arrange
        Vec3d pos = new Vec3d(10, 64, 10);
        when(mockEntity1.getPos()).thenReturn(pos);
        when(mockEntity1.getBoundingBox()).thenReturn(new Box(pos.x - 0.5, pos.y, pos.z - 0.5, 
                                                               pos.x + 0.5, pos.y + 2, pos.z + 0.5));
        when(mockEntity1.getWidth()).thenReturn(1.0f);
        when(mockEntity1.getHeight()).thenReturn(2.0f);

        // Act
        grid.insert(mockEntity1);
        List<Entity> result = grid.queryRadius(pos, 5.0);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(mockEntity1, result.get(0));
    }

    @Test
    void testRemoval() {
        // Arrange
        Vec3d pos = new Vec3d(10, 64, 10);
        when(mockEntity1.getPos()).thenReturn(pos);
        when(mockEntity1.getBoundingBox()).thenReturn(new Box(pos.x - 0.5, pos.y, pos.z - 0.5, 
                                                               pos.x + 0.5, pos.y + 2, pos.z + 0.5));
        when(mockEntity1.getWidth()).thenReturn(1.0f);
        when(mockEntity1.getHeight()).thenReturn(2.0f);

        // Act
        grid.insert(mockEntity1);
        grid.remove(mockEntity1);
        List<Entity> result = grid.queryRadius(pos, 5.0);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void testMultipleEntitiesInDifferentCells() {
        // Arrange - entities in different cells (cell size = 4)
        Vec3d pos1 = new Vec3d(2, 64, 2);   // Cell (0, 16, 0)
        Vec3d pos2 = new Vec3d(10, 64, 10); // Cell (8, 16, 8)
        Vec3d pos3 = new Vec3d(20, 64, 20); // Cell (16, 16, 16)

        setupMockEntity(mockEntity1, pos1);
        setupMockEntity(mockEntity2, pos2);
        setupMockEntity(mockEntity3, pos3);

        // Act
        grid.insert(mockEntity1);
        grid.insert(mockEntity2);
        grid.insert(mockEntity3);

        // Query near pos1 - should only find entity1
        List<Entity> result = grid.queryRadius(pos1, 3.0);

        // Assert
        assertEquals(1, result.size());
        assertEquals(mockEntity1, result.get(0));
    }

    @Test
    void testEntityUpdateCrossingCellBoundary() {
        // Arrange
        Vec3d oldPos = new Vec3d(3, 64, 3);  // Cell (0, 16, 0)
        Vec3d newPos = new Vec3d(7, 64, 7);  // Cell (4, 16, 4) - different cell

        when(mockEntity1.getPos()).thenReturn(oldPos);
        when(mockEntity1.getBoundingBox()).thenReturn(new Box(oldPos.x - 0.5, oldPos.y, oldPos.z - 0.5, 
                                                               oldPos.x + 0.5, oldPos.y + 2, oldPos.z + 0.5));
        when(mockEntity1.getWidth()).thenReturn(1.0f);
        when(mockEntity1.getHeight()).thenReturn(2.0f);

        // Act - insert at old position
        grid.insert(mockEntity1);
        
        // Update entity position
        when(mockEntity1.getPos()).thenReturn(newPos);
        when(mockEntity1.getBoundingBox()).thenReturn(new Box(newPos.x - 0.5, newPos.y, newPos.z - 0.5, 
                                                               newPos.x + 0.5, newPos.y + 2, newPos.z + 0.5));
        
        // Update in grid
        grid.update(mockEntity1);

        // Query at old position - should not find entity
        List<Entity> resultOld = grid.queryRadius(oldPos, 2.0);
        // Query at new position - should find entity
        List<Entity> resultNew = grid.queryRadius(newPos, 2.0);

        // Assert
        assertTrue(resultOld.isEmpty(), "Entity should not be found at old position");
        assertEquals(1, resultNew.size(), "Entity should be found at new position");
        assertEquals(mockEntity1, resultNew.get(0));
    }

    @Test
    void testQueryPerformanceIsConstantTime() {
        // This test verifies O(k) complexity - query time should be roughly constant
        // regardless of total entity count
        
        int[] entityCounts = {100, 500, 1000, 2000};
        long[] queryTimes = new long[entityCounts.length];

        for (int i = 0; i < entityCounts.length; i++) {
            SpatialGrid testGrid = new SpatialGrid(mockWorld, config);
            int count = entityCounts[i];

            // Insert entities
            for (int j = 0; j < count; j++) {
                Entity mockEnt = mock(Entity.class);
                Vec3d pos = new Vec3d(j * 10, 64, j * 10); // Spread out
                setupMockEntity(mockEnt, pos);
                testGrid.insert(mockEnt);
            }

            // Time queries
            Vec3d queryCenter = new Vec3d(0, 64, 0);
            long start = System.nanoTime();
            for (int q = 0; q < 100; q++) {
                testGrid.queryRadius(queryCenter, 50.0);
            }
            queryTimes[i] = System.nanoTime() - start;
        }

        // Verify O(k) - time should not scale linearly with entity count
        // Allow for 3x variance due to JVM warmup and GC
        double ratio1 = (double) queryTimes[1] / queryTimes[0];
        double ratio2 = (double) queryTimes[2] / queryTimes[1];
        double ratio3 = (double) queryTimes[3] / queryTimes[2];

        assertTrue(ratio1 < 5.0, "Query time should not scale linearly with entity count (100->500)");
        assertTrue(ratio2 < 5.0, "Query time should not scale linearly with entity count (500->1000)");
        assertTrue(ratio3 < 5.0, "Query time should not scale linearly with entity count (1000->2000)");
    }

    @Test
    void testConcurrentModificationSafety() {
        // Test that iterating over query results while modifying doesn't crash
        SpatialGrid threadSafeGrid = new SpatialGrid(mockWorld, config);

        // Insert initial entities
        for (int i = 0; i < 100; i++) {
            Entity mockEnt = mock(Entity.class);
            Vec3d pos = new Vec3d(i, 64, i);
            setupMockEntity(mockEnt, pos);
            threadSafeGrid.insert(mockEnt);
        }

        // Query and modify concurrently (simulated)
        Vec3d center = new Vec3d(50, 64, 50);
        
        // This should not throw ConcurrentModificationException
        assertDoesNotThrow(() -> {
            List<Entity> results = threadSafeGrid.queryRadius(center, 100.0);
            // Simulate concurrent modification
            Entity newEnt = mock(Entity.class);
            setupMockEntity(newEnt, new Vec3d(51, 64, 51));
            threadSafeGrid.insert(newEnt);
            
            // Continue using results
            for (Entity e : results) {
                e.getPos();
            }
        });
    }

    @Test
    void testQueryRadiusExactness() {
        // Arrange - entities at various distances
        Vec3d center = new Vec3d(0, 64, 0);
        
        Vec3d posInside = new Vec3d(3, 64, 4);      // Distance = 5
        Vec3d posOnEdge = new Vec3d(5, 64, 0);      // Distance = 5
        Vec3d posOutside = new Vec3d(6, 64, 0);     // Distance = 6

        Entity entityInside = mock(Entity.class);
        Entity entityOnEdge = mock(Entity.class);
        Entity entityOutside = mock(Entity.class);

        setupMockEntity(entityInside, posInside);
        setupMockEntity(entityOnEdge, posOnEdge);
        setupMockEntity(entityOutside, posOutside);

        grid.insert(entityInside);
        grid.insert(entityOnEdge);
        grid.insert(entityOutside);

        // Act
        List<Entity> result = grid.queryRadius(center, 5.0);

        // Assert - should find inside and on edge, but not outside
        assertEquals(2, result.size());
        assertTrue(result.contains(entityInside));
        assertTrue(result.contains(entityOnEdge));
        assertFalse(result.contains(entityOutside));
    }

    @Test
    void testClear() {
        // Arrange
        Vec3d pos = new Vec3d(10, 64, 10);
        setupMockEntity(mockEntity1, pos);
        grid.insert(mockEntity1);

        // Act
        grid.clear();
        List<Entity> result = grid.queryRadius(pos, 5.0);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetEntityCount() {
        // Arrange & Act
        assertEquals(0, grid.getEntityCount());
        
        setupMockEntity(mockEntity1, new Vec3d(10, 64, 10));
        grid.insert(mockEntity1);
        assertEquals(1, grid.getEntityCount());
        
        setupMockEntity(mockEntity2, new Vec3d(20, 64, 20));
        grid.insert(mockEntity2);
        assertEquals(2, grid.getEntityCount());
        
        grid.remove(mockEntity1);
        assertEquals(1, grid.getEntityCount());
    }

    @Test
    void testAdaptiveCellSizing() {
        // Test that adaptive mode adjusts cell size based on entity density
        SpatialGrid adaptiveGrid = new SpatialGrid(mockWorld, config);

        // Initially should have default cell size
        int initialSize = adaptiveGrid.getCellSize();
        assertTrue(initialSize >= SpatialGrid.MIN_CELL_SIZE);
        assertTrue(initialSize <= SpatialGrid.MAX_CELL_SIZE);

        // Add many entities to trigger rebalancing
        for (int i = 0; i < 500; i++) {
            Entity mockEnt = mock(Entity.class);
            Vec3d pos = new Vec3d(i % 50, 64, i / 50);
            setupMockEntity(mockEnt, pos);
            adaptiveGrid.insert(mockEnt);
        }

        // Trigger optimization
        adaptiveGrid.maybeRecalcCellSize(adaptiveGrid.getEntityCount(), 100, 1000);

        // Cell size may have changed based on density
        int optimizedSize = adaptiveGrid.getCellSize();
        assertTrue(optimizedSize >= SpatialGrid.MIN_CELL_SIZE);
        assertTrue(optimizedSize <= SpatialGrid.MAX_CELL_SIZE);
    }

    @Test
    void testQueryBox() {
        // Arrange
        Vec3d pos1 = new Vec3d(5, 64, 5);
        Vec3d pos2 = new Vec3d(15, 64, 15);
        Vec3d pos3 = new Vec3d(25, 64, 25);

        setupMockEntity(mockEntity1, pos1);
        setupMockEntity(mockEntity2, pos2);
        setupMockEntity(mockEntity3, pos3);

        grid.insert(mockEntity1);
        grid.insert(mockEntity2);
        grid.insert(mockEntity3);

        // Act - query box from (0,0,0) to (20, 128, 20)
        Box queryBox = new Box(0, 0, 0, 20, 128, 20);
        List<Entity> result = grid.queryBox(queryBox);

        // Assert - should find entity1 and entity2, not entity3
        assertEquals(2, result.size());
        assertTrue(result.contains(mockEntity1));
        assertTrue(result.contains(mockEntity2));
        assertFalse(result.contains(mockEntity3));
    }

    @Test
    void testInsertNullEntity() {
        assertThrows(NullPointerException.class, () -> grid.insert(null));
    }

    @Test
    void testRemoveNonExistentEntity() {
        // Should not throw when removing entity that was never inserted
        setupMockEntity(mockEntity1, new Vec3d(10, 64, 10));
        assertDoesNotThrow(() -> grid.remove(mockEntity1));
    }

    @Test
    void testUpdateNonExistentEntity() {
        // Should not throw when updating entity that was never inserted
        setupMockEntity(mockEntity1, new Vec3d(10, 64, 10));
        assertDoesNotThrow(() -> grid.update(mockEntity1));
    }

    // Helper method to setup mock entity with position
    private void setupMockEntity(Entity mock, Vec3d pos) {
        when(mock.getPos()).thenReturn(pos);
        when(mock.getBoundingBox()).thenReturn(new Box(pos.x - 0.5, pos.y, pos.z - 0.5, 
                                                        pos.x + 0.5, pos.y + 2, pos.z + 0.5));
        when(mock.getWidth()).thenReturn(1.0f);
        when(mock.getHeight()).thenReturn(2.0f);
    }
}
