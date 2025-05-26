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

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

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
        setupUI()
        setupListeners()

        // Tema switch ayarı
        val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        val isDark = prefs.getBoolean("dark_mode", false)
        binding.themeSwitch.isChecked = isDark
        binding.themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                prefs.edit().putBoolean("dark_mode", true).apply()
                Toast.makeText(requireContext(), "Koyu tema seçildi", Toast.LENGTH_SHORT).show()
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                prefs.edit().putBoolean("dark_mode", false).apply()
                Toast.makeText(requireContext(), "Açık tema seçildi", Toast.LENGTH_SHORT).show()
            }
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