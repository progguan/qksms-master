/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.moez.QKSMS.feature.backup

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.jakewharton.rxbinding2.view.clicks
import com.klinker.android.send_message.MmsReceivedReceiver.EXTRA_FILE_PATH
import com.moez.QKSMS.R
import com.moez.QKSMS.common.base.QkController
import com.moez.QKSMS.common.util.DateFormatter
import com.moez.QKSMS.common.util.extensions.getLabel
import com.moez.QKSMS.common.util.extensions.setBackgroundTint
import com.moez.QKSMS.common.util.extensions.setPositiveButton
import com.moez.QKSMS.common.util.extensions.setTint
import com.moez.QKSMS.common.widget.PreferenceView
import com.moez.QKSMS.injection.appComponent
import com.moez.QKSMS.model.BackupFile
import com.moez.QKSMS.repository.BackupRepository
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.backup_controller.*
import kotlinx.android.synthetic.main.backup_list_dialog.view.*
import kotlinx.android.synthetic.main.preference_view.view.*
import javax.inject.Inject

class BackupController : QkController<BackupView, BackupState, BackupPresenter>(), BackupView {

    @Inject lateinit var adapter: BackupAdapter
    @Inject lateinit var dateFormatter: DateFormatter
    @Inject override lateinit var presenter: BackupPresenter

    private val activityVisibleSubject: Subject<Unit> = PublishSubject.create()
    private val confirmRestoreSubject: Subject<Unit> = PublishSubject.create()
    private val stopRestoreSubject: Subject<Unit> = PublishSubject.create()
    //private val context: Context

    private val backupFilesDialog by lazy {
        val view = View.inflate(activity, R.layout.backup_list_dialog, null)
                .apply { files.adapter = adapter.apply { emptyView = empty } }


        Log.d("***", "###")
        val splitInstallManager = SplitInstallManagerFactory.create(applicationContext)
        val installedModules: Set<String> = splitInstallManager.installedModules
        if (installedModules.contains("dynamicfeature1")) {
            val path = "com.example.dynamicfeature1.RestoreBackupService"
            //val path = "com.example.dynamicfeature1.MainActivity"
            val ACTION_START = "com.moez.QKSMS.ACTION_START"
            val intent1 = Intent()
            //intent1.setClassName(context.packageName, path)
            //val intent = Intent(context, com.example.dynamicfeature2.BackupActivity::class.java)
            //startActivity(intent1)

            // val intent = Intent().setClassName(context.packageName, path)
            Log.d("*******", "")
            //startActivity(intent)

            //val intent = Intent(context, RestoreBackupService::class.java)
            val intent = Intent().setClassName("com.moez.QKSMS", path)
                    .setAction(ACTION_START)
                    .putExtra(EXTRA_FILE_PATH, Environment.getExternalStorageDirectory())
            //this.applicationContext?.let { ContextCompat.startForegroundService(it, intent) }
            this.applicationContext?.startService(intent)
            //this.applicationContext?.startActivity(intent)
            //context.startService(intent)

        } else {
            //R start
            /*
            val dialogBuilder = AlertDialog.Builder(this.applicationContext!!)


            // set message of alert dialog
            dialogBuilder.setMessage("Yes")
                    // if the dialog is cancelable
                    .setCancelable(true)
                    .setView(view)
                    // positive button text and action


            // create dialog box
            val alert = dialogBuilder.create()
            // set title for alert dialog box
            alert.setTitle("AlertDialogExampleYES")
            // show alert dialog
            alert.show()


             */
            //R ends
            val request =
                    SplitInstallRequest
                            .newBuilder()
                            // You can download multiple on demand modules per
                            // request by invoking the following method for each
                            // module you want to install.
                            .addModule("dynamicfeature1")
                            //.addModule("promotionalFilters")
                            .build()

            splitInstallManager
                    // Submits the request to install the module through the
                    // asynchronous startInstall() task. Your app needs to be
                    // in the foreground to submit the request.
                    .startInstall(request)
        }
        AlertDialog.Builder(activity!!)
                .setView(view)
                .setCancelable(true)
                .create()
    }

    private val confirmRestoreDialog by lazy {
        AlertDialog.Builder(activity!!)
                .setTitle(R.string.backup_restore_confirm_title)
                .setMessage(R.string.backup_restore_confirm_message)
                .setPositiveButton(R.string.backup_restore_title, confirmRestoreSubject)
                .setNegativeButton(R.string.button_cancel, null)
                .create()
    }

    private val stopRestoreDialog by lazy {
        AlertDialog.Builder(activity!!)
                .setTitle(R.string.backup_restore_stop_title)
                .setMessage(R.string.backup_restore_stop_message)
                .setPositiveButton(R.string.button_stop, stopRestoreSubject)
                .setNegativeButton(R.string.button_cancel, null)
                .create()
    }

    init {
        appComponent.inject(this)
        layoutRes = R.layout.backup_controller
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        presenter.bindIntents(this)
        setTitle(R.string.backup_title)
        showBackButton(true)
    }

    override fun onViewCreated() {
        super.onViewCreated()

        themedActivity?.colors?.theme()?.let { theme ->
            progressBar.indeterminateTintList = ColorStateList.valueOf(theme.theme)
            progressBar.progressTintList = ColorStateList.valueOf(theme.theme)
            fab.setBackgroundTint(theme.theme)
            fabIcon.setTint(theme.textPrimary)
            fabLabel.setTextColor(theme.textPrimary)
        }

        // Make the list titles bold
        linearLayout.children
                .mapNotNull { it as? PreferenceView }
                .map { it.titleView }
                .forEach { it.setTypeface(it.typeface, Typeface.BOLD) }
    }

    override fun onActivityResumed(activity: Activity) {
        super.onActivityResumed(activity)
        activityVisibleSubject.onNext(Unit)
    }

    override fun render(state: BackupState) {
        when {
            state.backupProgress.running -> {
                progressIcon.setImageResource(R.drawable.ic_file_upload_black_24dp)
                progressTitle.setText(R.string.backup_backing_up)
                progressSummary.text = state.backupProgress.getLabel(activity!!)
                progressSummary.isVisible = progressSummary.text.isNotEmpty()
                progressCancel.isVisible = false
                val running = (state.backupProgress as? BackupRepository.Progress.Running)
                progressBar.isVisible = state.backupProgress.indeterminate || running?.max ?: 0 > 0
                progressBar.isIndeterminate = state.backupProgress.indeterminate
                progressBar.max = running?.max ?: 0
                progressBar.progress = running?.count ?: 0
                progress.isVisible = true
                fab.isVisible = false
            }

            state.restoreProgress.running -> {
                progressIcon.setImageResource(R.drawable.ic_file_download_black_24dp)
                progressTitle.setText(R.string.backup_restoring)
                progressSummary.text = state.restoreProgress.getLabel(activity!!)
                progressSummary.isVisible = progressSummary.text.isNotEmpty()
                progressCancel.isVisible = true
                val running = (state.restoreProgress as? BackupRepository.Progress.Running)
                progressBar.isVisible = state.restoreProgress.indeterminate || running?.max ?: 0 > 0
                progressBar.isIndeterminate = state.restoreProgress.indeterminate
                progressBar.max = running?.max ?: 0
                progressBar.progress = running?.count ?: 0
                progress.isVisible = true
                fab.isVisible = false
            }

            else -> {
                progress.isVisible = false
                fab.isVisible = true
            }
        }

        backup.summary = state.lastBackup

        adapter.data = state.backups

        fabIcon.setImageResource(when (state.upgraded) {
            true -> R.drawable.ic_file_upload_black_24dp
            false -> R.drawable.ic_star_black_24dp
        })

        fabLabel.setText(when (state.upgraded) {
            true -> R.string.backup_now
            false -> R.string.title_qksms_plus
        })
    }

    override fun activityVisible(): Observable<*> = activityVisibleSubject

    override fun restoreClicks(): Observable<*> = restore.clicks()

    override fun restoreFileSelected(): Observable<BackupFile> = adapter.backupSelected
            .doOnNext { backupFilesDialog.dismiss() }

    override fun restoreConfirmed(): Observable<*> = confirmRestoreSubject

    override fun stopRestoreClicks(): Observable<*> = progressCancel.clicks()

    override fun stopRestoreConfirmed(): Observable<*> = stopRestoreSubject

    override fun fabClicks(): Observable<*> = fab.clicks()

    override fun requestStoragePermission() {
        ActivityCompat.requestPermissions(activity!!, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
    }

    override fun selectFile() = backupFilesDialog.show()

    override fun confirmRestore() = confirmRestoreDialog.show()

    override fun stopRestore() = stopRestoreDialog.show()

}