package com.mphasis.rmi;

import java.rmi.RemoteException;

public class Authenticationrml implements AuthenticationRemote{
    @Override
    public boolean validateUser(String username, String password) throws RemoteException {
        if(username.equalsIgnoreCase("Rahul") && password.equals("pass")){
            return true;
        }
        return false;

    }
}
