package com.example.personalfinances.data.repository

import android.content.Context
import android.net.Uri
import com.example.personalfinances.data.export.AnnualReportWorkbook
import com.example.personalfinances.domain.model.OperationResult
import com.example.personalfinances.domain.report.AnnualReport
import com.example.personalfinances.domain.repository.ReportRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

/**
 * [ReportRepository] that writes Excel workbooks through Android's content resolver, so the user
 * can save to any location the system file picker offers. Building the workbook is pure Kotlin
 * (see [AnnualReportWorkbook]); this class only does the file I/O.
 */
class ReportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ReportRepository {

    override suspend fun writeAnnualReport(destination: String, report: AnnualReport): OperationResult =
        withContext(Dispatchers.IO) {
            try {
                val bytes = AnnualReportWorkbook.build(report).toBytes()
                // "wt" truncates, so overwriting an existing, longer file leaves no stale tail.
                val output = context.contentResolver.openOutputStream(Uri.parse(destination), "wt")
                    ?: return@withContext OperationResult.Failure("Could not open that file for writing.")
                output.use { it.write(bytes) }
                OperationResult.Success
            } catch (e: IOException) {
                OperationResult.Failure("Could not write the report: ${e.message ?: "unknown error"}")
            } catch (e: SecurityException) {
                OperationResult.Failure("The app is not allowed to write to that location.")
            }
        }
}
