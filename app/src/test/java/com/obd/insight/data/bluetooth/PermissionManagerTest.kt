package com.obd.insight.data.bluetooth

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PermissionManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = mockk()
    }

    @Test
    fun `hasBluetoothPermissions returns true when all permissions granted`() {
        every { context.checkPermission(Manifest.permission.BLUETOOTH_SCAN, any(), any()) } returns PackageManager.PERMISSION_GRANTED
        every { context.checkPermission(Manifest.permission.BLUETOOTH_CONNECT, any(), any()) } returns PackageManager.PERMISSION_GRANTED

        val manager = PermissionManager(context, sdkInt = Build.VERSION_CODES.S)

        assertTrue(manager.hasBluetoothPermissions())
    }

    @Test
    fun `hasBluetoothPermissions returns false when permission denied`() {
        every { context.checkPermission(Manifest.permission.BLUETOOTH_SCAN, any(), any()) } returns PackageManager.PERMISSION_DENIED
        every { context.checkPermission(Manifest.permission.BLUETOOTH_CONNECT, any(), any()) } returns PackageManager.PERMISSION_GRANTED

        val manager = PermissionManager(context, sdkInt = Build.VERSION_CODES.S)

        assertFalse(manager.hasBluetoothPermissions())
    }

    @Test
    fun `requiredPermissions returns BLUETOOTH_SCAN and BLUETOOTH_CONNECT`() {
        val manager = PermissionManager(context, sdkInt = Build.VERSION_CODES.S)
        val permissions = manager.requiredPermissions()

        assertTrue(permissions.contains(Manifest.permission.BLUETOOTH_SCAN))
        assertTrue(permissions.contains(Manifest.permission.BLUETOOTH_CONNECT))
    }
}
