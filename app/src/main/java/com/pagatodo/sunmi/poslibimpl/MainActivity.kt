package com.pagatodo.sunmi.poslibimpl

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import com.pagatodo.sunmi.poslib.PosLib
import com.pagatodo.sunmi.poslib.SunmiTrxWrapper
import com.pagatodo.sunmi.poslib.config.PosConfig
import com.pagatodo.sunmi.poslib.interfaces.AppEmvSelectListener
import com.pagatodo.sunmi.poslib.interfaces.OnClickAcceptListener
import com.pagatodo.sunmi.poslib.interfaces.SunmiTrxListener
import com.pagatodo.sunmi.poslib.model.*
import com.pagatodo.sunmi.poslib.util.Constants
import com.pagatodo.sunmi.poslib.util.EmvUtil
import com.pagatodo.sunmi.poslib.util.PosLogger
import com.pagatodo.sunmi.poslib.util.PosResult
import com.pagatodo.sunmi.poslib.viewmodel.SunmiViewModel
import com.pagatodo.sunmi.poslib.util.LoadFile
import com.sunmi.pay.hardware.aidl.AidlConstants
import com.sunmi.pay.hardware.aidlv2.bean.PinPadConfigV2
import com.sunmi.pay.hardware.aidlv2.pinpad.PinPadListenerV2
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.fullcarga.android.api.data.respuesta.OperacionSiguiente
import net.fullcarga.android.api.data.respuesta.Respuesta

class MainActivity : AppCompatActivity(), SunmiTrxListener<String> {

    private val viewMPci by lazy { ViewModelProvider(this)[ViewModelPci::class.java] }
    private val trxManager by lazy { SunmiTrxWrapper(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        PosLib.createInstance(this)
        btnAccept.setOnClickListener {
            if (amountTxv.text.isNotEmpty() && amountTxv.text.isDigitsOnly())
                trxManager.initTransaction()
        }
    }

    override fun onStart() {
        super.onStart()
        initTerminal()
    }

    private fun initTerminal() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                delay(2000L)
                val fileAid = resources.openRawResource(R.raw.aids_es_1_2)
                val fileCapk = resources.openRawResource(R.raw.capks_es_1_2)
                val fileDrl = resources.openRawResource(R.raw.dlrs_es_1_0)
                val aidList = LoadFile.readConfigFile<Aid>(fileAid)
                val capkList = LoadFile.readConfigFile<Capk>(fileCapk)
                val drlsList = LoadFile.readConfigFile<Drl>(fileDrl)
                val posConfig = PosConfig()
                posConfig.aids = aidList
                posConfig.capks = capkList
                posConfig.drls = drlsList
                PosLib.loadGlobalConfig(posConfig)
                Log.i("MainActivity", "configure terminal success")
            } catch (e: Exception) {
                PosLogger.e("MainActivity", e.toString())
            }
        }
    }

    override fun onDialogRequestCard(message: String?) {
        askForCard?.show()
    }

    override fun onDismissRequestCard() {
        askForCard?.dismiss()
    }

    override fun onDialogProcessOnline(message: String?) {
        dialogProgress.show(supportFragmentManager, dialogProgress.tag)
    }

    override fun onDismissRequestOnline() {
        if (dialogProgress.isAdded) dialogProgress.dismiss()
    }

    override fun onShowSingDialog(responseTrx: Respuesta, dataCard: DataCard) {
        Toast.makeText(this, "Mostrar dialogo de firma", Toast.LENGTH_LONG).show()
    }

    override fun createTransactionData() = TransactionData().apply {
        transType = Constants.TransType.PURCHASE
        amount = amountTxv.text.toString()
        totalAmount = amountTxv.text.toString()
        otherAmount = "00"
        currencyCode = "0156"
        cashBackAmount = "00"
        taxes = "00"
        comisions = "00"
        gratuity = "00"
        sigmaOperation = "V"
        tagsEmv = EmvUtil.tagsDefault.toList()
    }

    override fun pinMustBeForced(): Boolean {
        return true
    }

    override fun checkCardTypes(): Int {
        return AidlConstants.CardType.MAGNETIC.value or AidlConstants.CardType.IC.value or AidlConstants.CardType.NFC.value
    }

    override fun onShowTicketDialog(singBytes: ByteArray?, responseTrx: Respuesta, dataCard: DataCard) {
        Toast.makeText(this, "Mostrar dialogo de ticket", Toast.LENGTH_LONG).show()
    }

    override fun onShowPinPadDialog(pinPadListener: PinPadListenerV2.Stub, pinPadConfig: PinPadConfigV2) {
        val pinPadDialog = PinPadDialog.createInstance(pinPadConfig)
        pinPadDialog.setPasswordLength(6)
        pinPadDialog.setTextAccept("Aceptar")
        pinPadDialog.setTextCancel("Cancelar")
        pinPadDialog.setPinPadListenerV2(pinPadListener)

        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        transaction.add(android.R.id.content, pinPadDialog, pinPadDialog.tag).commit()
    }

    override fun onShowSelectApp(listEmvApps: List<String>, applicationEmv: AppEmvSelectListener) {
        TODO("Not yet implemented")
    }

    override fun onSync(dataCard: DataCard) {
        viewMPci.sync()
    }

    override fun onFailure(error: PosResult, listener: OnClickAcceptListener?) {
        Toast.makeText(this, error.message, Toast.LENGTH_LONG).show()
    }

    override fun onPurchase(dataCard: DataCard) {
        viewMPci.purchase()
    }

    override fun doOperationNext(nextOperation: OperacionSiguiente, nextOprResult: PosResult) {
        Toast.makeText(this, "Operacion Siguiente", Toast.LENGTH_LONG).show()
    }

    override fun getVmodelPCI() = viewMPci

    private val dialogProgress: DialogProgress by lazy {
        DialogProgress().apply { isCancelable = false }
    }

    private val askForCard: AlertDialog? by lazy {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setMessage("Por favor inserta, desliza o acerca la tarjeta.")
        builder.create()
    }

    override fun onShowDniDialog(dataCard: DataCard) {
        TODO("Not yet implemented")
    }

    override fun onShowZipDialog(dataCard: DataCard) {
        TODO("Not yet implemented")
    }

    override fun showReading() {
        TODO("Not yet implemented")
    }

    override fun showRemoveCard(dataCard: DataCard?) {
        TODO("Not yet implemented")
    }
}