package com.app.rewardsplanet.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.app.rewardsplanet.models.UserModel;
import com.app.rewardsplanet.repository.UserRepository;

public class UserViewModel extends AndroidViewModel {

    private final UserRepository userRepository;

    public UserViewModel(@NonNull Application application) {
        super(application);
        userRepository = UserRepository.getInstance(application);
    }

    public LiveData<UserModel> getUser() {
        return userRepository.getUser();
    }

    public LiveData<Boolean> getLoading() {
        return userRepository.getLoading();
    }

    public LiveData<String> getError() {
        return userRepository.getError();
    }

    public UserModel getCurrentUserModel() {
        return userRepository.getCurrentUserModel();
    }

    public void loadUser(String uid, UserRepository.UserLoadCallback callback) {
        userRepository.loadCurrentUser(uid, callback);
    }

    public void refreshUser() {
        userRepository.refreshCurrentUser();
    }

    public void updateLocalUser(UserModel user) {
        userRepository.updateLocalUser(user);
    }

    public void clearUser() {
        userRepository.clearUser();
    }
}
