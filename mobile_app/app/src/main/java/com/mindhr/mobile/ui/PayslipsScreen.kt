package com.mindhr.mobile.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mindhr.mobile.data.Payslip
import com.mindhr.mobile.data.Session
import com.mindhr.mobile.ui.theme.MhColors

@Composable
fun PayslipsScreen(onBack: () -> Unit) {
    val me = Session.employeeId
    val loader = rememberLoad { if (me == null) emptyList() else Session.api.payslips(me).sortedByDescending { it.id } }
    var open by rememberSaveable { mutableStateOf<Long?>(null) }

    Scaffold(topBar = { BackTopBar("Payslips", onBack) }, containerColor = MhColors.Background) { padding ->
        LoadContent(loader, Modifier.padding(padding)) { slips ->
            if (slips.isEmpty()) {
                MessageState(icon = Icons.Outlined.ReceiptLong, message = "No payslips yet. They appear here after HR runs payroll.", modifier = Modifier.padding(padding))
                return@LoadContent
            }
            LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(slips, key = { it.id }) { p ->
                    PayslipCard(p, expanded = open == p.id) { open = if (open == p.id) null else p.id }
                }
            }
        }
    }
}

@Composable
private fun PayslipCard(p: Payslip, expanded: Boolean, onToggle: () -> Unit) {
    SectionCard(Modifier.clickable(onClick = onToggle)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Payroll run #${p.payrollRunId ?: p.id}", fontWeight = FontWeight.SemiBold)
                Text(
                    "${p.daysPresent} present · ${p.daysAbsent} absent · ${p.daysLate} late",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatMoney(p.netSalary), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MhColors.Primary)
                p.status?.let { StatusChip(it) }
            }
        }
        AnimatedVisibility(expanded) {
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                HorizontalDivider()
                Text("Earnings", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
                Line("Basic salary", p.basicSalary)
                Line("Allowances", p.allowances)
                Line("Overtime", p.overtime)
                Line("Bonus", p.bonus)
                Line("Commission", p.commission)
                Line("Gross salary", p.grossSalary, bold = true)
                Text("Deductions", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
                Line("Late", -p.lateDeduction)
                Line("Absence", -p.absentDeduction)
                Line("Loan", -p.loanDeduction)
                Line("Salary advance", -p.advanceDeduction)
                HorizontalDivider()
                Line("Net salary", p.netSalary, bold = true, color = MhColors.Primary)
            }
        }
    }
}

@Composable
private fun Line(label: String, amount: Double, bold: Boolean = false, color: Color = Color.Unspecified) {
    if (amount == 0.0 && !bold) return
    Row {
        Text(label, Modifier.weight(1f), fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal)
        Text(formatMoney(amount), fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal, color = if (amount < 0) MhColors.Red else color)
    }
}
