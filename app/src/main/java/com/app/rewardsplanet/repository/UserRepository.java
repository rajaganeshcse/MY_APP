package com.app.rewardsplanet.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.app.rewardsplanet.UserPref;
import com.app.rewardsplanet.models.UserModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class UserRepository {

    private static volatile UserRepository instance;

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private UserPref userPref;

    private final MutableLiveData<UserModel> userLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    private ListenerRegistration snapshotListener;
    private String activeUid;

    public interface UserLoadCallback {
        void onSuccess(UserModel user);
        void onError(String errorMessage);
    }

    private UserRepository(Context context) {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        if (context != null) {
            userPref = new UserPref(context.getApplicationContext());
        }
    }

    public static UserRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (UserRepository.class) {
                if (instance == null) {
                    instance = new UserRepository(context);
                }
            }
        }
        return instance;
    }

    /* ================= LIVEDATA EXPOSURE ================= */

    public LiveData<UserModel> getUser() {
        return userLiveData;
    }

    public LiveData<Boolean> getLoading() {
        return loadingLiveData;
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    public UserModel getCurrentUserModel() {
        return userLiveData.getValue();
    }

    public String getCurrentUid() {
        FirebaseUser currentUser = auth.getCurrentUser();
        return currentUser != null ? currentUser.getUid() : "";
    }

    /* ================= INITIALIZATION & REALTIME SNAPSHOT ================= */

    public void loadCurrentUser(String uid, UserLoadCallback callback) {
        if (uid == null || uid.isEmpty()) {
            loadingLiveData.postValue(false);
            if (callback != null) callback.onError("Invalid UID");
            return;
        }

        activeUid = uid;
        loadingLiveData.postValue(true);

        // Remove previous listener if UID changed
        detachListener();

        DocumentReference userDocRef = db.collection("users").document(uid);

        // Attach centralized single Firestore snapshot listener
        snapshotListener = userDocRef.addSnapshotListener((documentSnapshot, error) -> {
            loadingLiveData.postValue(false);

            if (error != null) {
                errorLiveData.postValue(error.getMessage());
                if (callback != null) callback.onError(error.getMessage());
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                try {
                    UserModel userModel = documentSnapshot.toObject(UserModel.class);
                    if (userModel != null) {
                        userModel.setUid(uid);
                        userLiveData.postValue(userModel);
                        syncUserPref(userModel);

                        if (callback != null) callback.onSuccess(userModel);
                    }
                } catch (Exception e) {
                    errorLiveData.postValue(e.getMessage());
                    if (callback != null) callback.onError(e.getMessage());
                }
            } else {
                String msg = "User document does not exist in Firestore";
                errorLiveData.postValue(msg);
                if (callback != null) callback.onError(msg);
            }
        });
    }

    /* ================= LOCAL CACHE SYNC ================= */

    private void syncUserPref(UserModel user) {
        if (userPref == null || user == null) return;

        userPref.setUid(user.getUid());
        userPref.setName(user.getName());
        userPref.setEmail(user.getEmail());
        userPref.setPhone(user.getPhone());
        userPref.setGender(user.getGender());
        userPref.setDob(user.getDob());
        userPref.setProfileImage(user.getProfile_pic());
        userPref.setReferralCode(user.getReferralCode());
        userPref.setCoins(user.getCoins());
        userPref.setTickets((int) user.getTickets());
        userPref.setWalletToken((int) user.getTickets());
        userPref.setLogin(true);
    }

    /* ================= REFRESH ================= */

    public void refreshCurrentUser() {
        String uid = getCurrentUid();
        if (!uid.isEmpty()) {
            loadCurrentUser(uid, null);
        }
    }

    /* ================= UPDATE LOCAL IN-MEMORY STATE ================= */

    public void updateLocalUser(UserModel user) {
        if (user != null) {
            userLiveData.postValue(user);
            syncUserPref(user);
        }
    }

    /* ================= LOGOUT / CLEAR ================= */

    public void clearUser() {
        detachListener();
        activeUid = null;
        userLiveData.postValue(null);

        if (userPref != null) {
            userPref.logout();
        }
    }

    private void detachListener() {
        if (snapshotListener != null) {
            snapshotListener.remove();
            snapshotListener = null;
        }
    }
}
