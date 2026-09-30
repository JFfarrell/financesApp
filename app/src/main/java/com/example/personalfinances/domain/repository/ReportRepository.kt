package com.example.personalfinances.domain.repository

import com.example.personalfinances.domain.model.OperationResult
import com.example.personalfinances.domain.report.AnnualReport

/**
 * Writes reports to files the user chooses. Locations are opaque strings (a document address
 * from the system file picker), which keeps this interface free of Android types.
 */
interface ReportRepository {
    /** Writes [report] as an Excel workbook to [destination], replacing the file's contents. */
    suspend fun writeAnnualReport(destination: String, report: AnnualReport): OperationResult
}
