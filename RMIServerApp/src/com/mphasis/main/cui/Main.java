package com.mphasis.main.cui;

import com.mphasis.rmi.AuthenticationRemote;
import com.mphasis.rmi.Authenticationrml;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Main {

    public static void main(String[] args) {
	// write your code here
        Authenticationrml authenticationrml = new Authenticationrml();
        try {
            AuthenticationRemote stub = (AuthenticationRemote) UnicastRemoteObject.exportObject(authenticationrml);
            Registry registry = LocateRegistry.getRegistry();
            registry.bind("authen", stub);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        catch (AlreadyBoundException e){
            e.printStackTrace();
        }
        System.err.printf("Server ready");
    }
}
