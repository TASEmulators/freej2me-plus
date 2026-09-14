/*
	This file is part of FreeJ2ME.

	FreeJ2ME is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	FreeJ2ME is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with FreeJ2ME.  If not, see http://www.gnu.org/licenses/
*/
package javax.microedition.pki;

public final class UserCredentialManager
{

    public static final String ALGORITHM_RSA = "1.2.840.113549.1.1.1";
    public static final String ALGORITHM_DSA = "1.2.840.10040.4.1";
    public static final int KEY_USAGE_AUTHENTICATION = 0;
    public static final int KEY_USAGE_NON_REPUDIATION = 1;

    public static byte[] generateCSR(String nameInfo, String algorithm, int keyLen, int keyUsage,
                                      String securityElementID, String securityElementPrompt,
                                      boolean forceKeyGen) throws UserCredentialManagerException, javax.microedition.securityservice.CMSMessageSignatureServiceException
    {
        if (nameInfo == null || algorithm == null || keyLen <= 0 || (keyUsage != KEY_USAGE_AUTHENTICATION && keyUsage != KEY_USAGE_NON_REPUDIATION))
        {
            throw new IllegalArgumentException("Invalid parameters");
        }

        return new byte[0];
    }

    public static boolean addCredential(String certDisplayName, byte[] pkiPath, String uri) throws UserCredentialManagerException
    {
        if (certDisplayName == null || certDisplayName.length() == 0 || pkiPath == null) { throw new IllegalArgumentException("Invalid parameters"); }

        return true;
    }

    public static boolean removeCredential(String certDisplayName, byte[] issuerAndSerialNumber,
                                            String securityElementID, String securityElementPrompt) throws UserCredentialManagerException
    {
        if (certDisplayName == null || certDisplayName.length() == 0 || issuerAndSerialNumber == null) { throw new IllegalArgumentException("Invalid parameters"); }

        return true;
    }
}
