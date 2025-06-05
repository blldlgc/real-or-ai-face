package com.example.derinogrenme.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.derinogrenme.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import androidx.appcompat.app.AppCompatDelegate
import android.content.Context
import android.content.Intent
import com.example.derinogrenme.LoginActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.EmailAuthProvider
import android.util.Log
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.example.derinogrenme.R
import android.app.Dialog
import com.example.derinogrenme.utils.NotificationHelper
import com.example.derinogrenme.utils.ThemeManager
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import java.util.Calendar
import java.util.Date

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var themeManager: ThemeManager

    companion object {
        private const val NOTIFICATION_PERMISSION_CODE = 123
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            // Kullanıcı yoksa login ekranına yönlendir
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
            return
        }
        binding.profileName.text = user.displayName ?: "Kullanıcı"
        binding.profileEmail.text = user.email ?: "-"

        auth = FirebaseAuth.getInstance()
        notificationHelper = NotificationHelper(requireContext())
        themeManager = ThemeManager(requireContext())
        setupUI()
        setupListeners()

        // Tema switch ayarı
        binding.themeSwitch.isChecked = themeManager.getThemeMode()
        binding.themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            themeManager.saveThemeMode(isChecked)
            themeManager.applyThemeMode(isChecked)
            Toast.makeText(
                requireContext(),
                if (isChecked) "Koyu tema seçildi" else "Açık tema seçildi",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupUI() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            binding.profileName.text = currentUser.displayName ?: "Kullanıcı"
            binding.profileEmail.text = currentUser.email
        }
    }

    private fun setupListeners() {
        binding.changePasswordButton.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.logoutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        // Bildirim test butonları
        binding.testDailySummaryButton.setOnClickListener {
            if (checkNotificationPermission()) {
                // Gerçek tahmin verilerini getir
                val db = FirebaseFirestore.getInstance()
                
                // Bugünün başlangıcını hesapla
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = Timestamp(calendar.time)
                
                Log.d("ProfileFragment", "Sorgu başlatılıyor - userId: ${auth.currentUser?.uid}, startOfDay: $startOfDay")
                
                db.collection("predictions")
                    .whereEqualTo("userId", auth.currentUser?.uid)
                    .whereGreaterThanOrEqualTo("timestamp", startOfDay)
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { documents ->
                        Log.d("ProfileFragment", "Sorgu başarılı - Bulunan döküman sayısı: ${documents.size()}")
                        val predictionCount = documents.size()
                        if (predictionCount > 0) {
                            // Güven oranlarının ortalamasını hesapla
                            val totalConfidence = documents.sumOf { doc ->
                                val confidence = doc.getDouble("confidence") ?: 0.0
                                Log.d("ProfileFragment", "Döküman confidence değeri: $confidence")
                                confidence
                            }
                            val averageConfidence = (totalConfidence / predictionCount * 100).toInt()
                            
                            Log.d("ProfileFragment", "Tahmin istatistikleri - Toplam: $predictionCount, Toplam Güven: $totalConfidence, Ortalama Güven: $averageConfidence")
                            
                            notificationHelper.showDailySummaryNotification(predictionCount, averageConfidence)
                            Toast.makeText(requireContext(), "Günlük özet bildirimi gönderildi", Toast.LENGTH_SHORT).show()
                        } else {
                            Log.d("ProfileFragment", "Bugün için tahmin bulunamadı")
                            Toast.makeText(requireContext(), "Bugün henüz tahmin yapılmamış", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("ProfileFragment", "Veri getirme hatası", e)
                        Log.e("ProfileFragment", "Hata detayı: ${e.message}")
                        Log.e("ProfileFragment", "Hata tipi: ${e.javaClass.simpleName}")
                        e.printStackTrace()
                        Toast.makeText(requireContext(), "Veriler alınırken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        binding.testReminderButton.setOnClickListener {
            if (checkNotificationPermission()) {
                // Son 24 saatteki tahminleri kontrol et
                val db = FirebaseFirestore.getInstance()
                
                // 24 saat öncesini hesapla
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.HOUR, -24)
                val dayAgo = Timestamp(calendar.time)
                
                Log.d("ProfileFragment", "Hatırlatma sorgusu başlatılıyor - userId: ${auth.currentUser?.uid}, dayAgo: $dayAgo")
                
                db.collection("predictions")
                    .whereEqualTo("userId", auth.currentUser?.uid)
                    .whereGreaterThanOrEqualTo("timestamp", dayAgo)
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { documents ->
                        Log.d("ProfileFragment", "Hatırlatma sorgusu başarılı - Döküman var mı: ${!documents.isEmpty}")
                        if (documents.isEmpty) {
                            notificationHelper.showReminderNotification()
                            Toast.makeText(requireContext(), "Hatırlatma bildirimi gönderildi", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "Son 24 saatte tahmin yapılmış", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("ProfileFragment", "Hatırlatma veri getirme hatası", e)
                        Log.e("ProfileFragment", "Hata detayı: ${e.message}")
                        Log.e("ProfileFragment", "Hata tipi: ${e.javaClass.simpleName}")
                        e.printStackTrace()
                        Toast.makeText(requireContext(), "Veriler alınırken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun checkNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // İzin yoksa, izin iste
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            NOTIFICATION_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(requireContext(), "Bildirim izni verildi", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Bildirim izni verilmedi. Bildirimler çalışmayacak.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val currentPasswordEdit = dialogView.findViewById<TextInputEditText>(R.id.currentPasswordEdit)
        val newPasswordEdit = dialogView.findViewById<TextInputEditText>(R.id.newPasswordEdit)
        val confirmPasswordEdit = dialogView.findViewById<TextInputEditText>(R.id.confirmPasswordEdit)
        
        val currentPasswordLayout = dialogView.findViewById<TextInputLayout>(R.id.currentPasswordLayout)
        val newPasswordLayout = dialogView.findViewById<TextInputLayout>(R.id.newPasswordLayout)
        val confirmPasswordLayout = dialogView.findViewById<TextInputLayout>(R.id.confirmPasswordLayout)

        dialogView.findViewById<View>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.changePasswordButton).setOnClickListener {
            val currentPassword = currentPasswordEdit.text.toString()
            val newPassword = newPasswordEdit.text.toString()
            val confirmPassword = confirmPasswordEdit.text.toString()

            // Hata mesajlarını temizle
            currentPasswordLayout.error = null
            newPasswordLayout.error = null
            confirmPasswordLayout.error = null

            // Validasyonlar
            if (currentPassword.isEmpty()) {
                currentPasswordLayout.error = "Mevcut şifre boş olamaz"
                return@setOnClickListener
            }

            if (newPassword.isEmpty()) {
                newPasswordLayout.error = "Yeni şifre boş olamaz"
                return@setOnClickListener
            }

            if (newPassword.length < 6) {
                newPasswordLayout.error = "Şifre en az 6 karakter olmalıdır"
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                confirmPasswordLayout.error = "Şifreler eşleşmiyor"
                return@setOnClickListener
            }

            // Şifre değiştirme işlemi
            val user = auth.currentUser
            val email = user?.email

            if (email != null) {
                val credential = EmailAuthProvider.getCredential(email, currentPassword)
                user.reauthenticate(credential)
                    .addOnCompleteListener { reauthTask ->
                        if (reauthTask.isSuccessful) {
                            user.updatePassword(newPassword)
                                .addOnCompleteListener { updateTask ->
                                    if (updateTask.isSuccessful) {
                                        Toast.makeText(
                                            requireContext(),
                                            "Şifre başarıyla değiştirildi",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        dialog.dismiss()
                                    } else {
                                        Log.e("ProfileFragment", "Şifre güncellenirken hata", updateTask.exception)
                                        Toast.makeText(
                                            requireContext(),
                                            "Şifre değiştirilirken bir hata oluştu: ${updateTask.exception?.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                        } else {
                            Log.e("ProfileFragment", "Kullanıcı doğrulanırken hata", reauthTask.exception)
                            currentPasswordLayout.error = "Mevcut şifre yanlış"
                        }
                    }
            }
        }

        dialog.show()
    }

    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Çıkış Yap")
            .setMessage("Çıkış yapmak istediğinizden emin misiniz?")
            .setPositiveButton("Evet") { _, _ ->
                auth.signOut()
                // Ana sayfaya yönlendir
                requireActivity().finish()
            }
            .setNegativeButton("Hayır", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 