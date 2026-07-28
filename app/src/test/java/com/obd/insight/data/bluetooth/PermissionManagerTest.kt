package com.obd.insight.data.bluetooth

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PermissionManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = mockk()
        mockkStatic(ContextCompat::class)
    }

    @Test
    fun `hasBluetoothPermissions returns true when all permissions granted`() {
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
        } returns PackageManager.PERMISSION_GRANTED
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
        } returns PackageManager.PERMISSION_GRANTED

        val manager = PermissionManager(context)

        assertTrue(manager.hasBluetoothPermissions())
    }

    @Test
    fun `hasBluetoothPermissions returns false when permission denied`() {
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
        } returns PackageManager.PERMISSION_DENIED

        val manager = PermissionManager(context)

        assertFalse(manager.hasBluetoothPermissions())
    }

    @Test
    fun `requiredPermissions returns BLUETOOTH_SCAN and BLUETOOTH_CONNECT`() {
        val manager = PermissionManager(context)
        val permissions = manager.requiredPermissions()

        assertTrue(permissions.contains(Manifest.permission.BLUETOOTH_SCAN))
        assertTrue(permissions.contains(Manifest.permission.BLUETOOTH_CONNECT))
    }
}
