package com.obd.insight.di

import android.content.Context
import androidx.room.Room
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.obd.insight.data.bluetooth.BluetoothConnectionManager
import com.obd.insight.data.bluetooth.PermissionManager
import com.obd.insight.data.elm327.Elm327Protocol
import com.obd.insight.data.obd.ObdPidReader
import com.obd.insight.data.obd.ObdSensorReader
import com.obd.insight.data.persistence.ObdDatabase
import com.obd.insight.data.persistence.TripRepository
import com.obd.insight.ui.connection.ConnectionViewModel
import com.obd.insight.ui.dashboard.DashboardViewModel
import com.obd.insight.ui.history.TripHistoryViewModel
import com.obd.insight.ui.history.TripDetailsViewModel
import com.obd.insight.ui.terminal.AtTerminalViewModel

object AppModule {

    private var appContext: Context? = null
    private var bluetoothManager: BluetoothConnectionManager? = null
    private var elm327Protocol: Elm327Protocol? = null
    private var obdPidReader: ObdPidReader? = null
    private var obdSensorReader: ObdSensorReader? = null
    private var permissionManager: PermissionManager? = null
    private var database: ObdDatabase? = null
    private var tripRepository: TripRepository? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun provideBluetoothManager(): BluetoothConnectionManager {
        return bluetoothManager ?: BluetoothConnectionManager(checkNotNull(appContext)).also {
            bluetoothManager = it
        }
    }

    fun provideElm327Protocol(): Elm327Protocol {
        return elm327Protocol ?: Elm327Protocol(provideBluetoothManager()).also {
            elm327Protocol = it
        }
    }

    fun provideObdPidReader(): ObdPidReader {
        return obdPidReader ?: ObdPidReader(provideElm327Protocol()).also {
            obdPidReader = it
        }
    }

    fun provideObdSensorReader(): ObdSensorReader {
        return obdSensorReader ?: ObdSensorReader(
            pidReader = provideObdPidReader(),
            isConnected = { provideBluetoothManager().state.value is com.obd.insight.domain.model.ConnectionState.Connected }
        ).also {
            obdSensorReader = it
        }
    }

    fun providePermissionManager(context: Context): PermissionManager {
        return permissionManager ?: PermissionManager(context).also {
            permissionManager = it
        }
    }

    fun provideTripRepository(context: Context): TripRepository {
        return tripRepository ?: TripRepository(
            tripDao = provideDatabase(context).tripDao(),
            sensorReadingDao = provideDatabase(context).sensorReadingDao()
        ).also { tripRepository = it }
    }

    private fun provideDatabase(context: Context): ObdDatabase {
        return database ?: Room.databaseBuilder(
            context.applicationContext,
            ObdDatabase::class.java,
            "obd-insight.db"
        ).addMigrations(ObdDatabase.MIGRATION_1_2).build().also { database = it }
    }

    val viewModelFactory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            ConnectionViewModel(
                bluetoothManager = provideBluetoothManager(),
                elm327Protocol = provideElm327Protocol(),
                obdPidReader = provideObdPidReader(),
                sensorReader = provideObdSensorReader()
            )
        }
        initializer {
            AtTerminalViewModel(
                bluetoothManager = provideBluetoothManager()
            )
        }
    }

    fun dashboardViewModelFactory(context: Context): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            DashboardViewModel(
                sensorReader = provideObdSensorReader(),
                tripRepository = provideTripRepository(context)
            )
        }
    }

    fun tripHistoryViewModelFactory(context: Context): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            TripHistoryViewModel(
                repository = provideTripRepository(context)
            )
        }
    }

    fun tripDetailsViewModelFactory(context: Context, tripId: Long): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            TripDetailsViewModel(
                tripId = tripId,
                repository = provideTripRepository(context)
            )
        }
    }

    fun cleanup() {
        bluetoothManager?.close()
        bluetoothManager = null
        elm327Protocol = null
        obdPidReader = null
        obdSensorReader = null
        permissionManager = null
        database?.close()
        database = null
        tripRepository = null
        appContext = null
    }
}
