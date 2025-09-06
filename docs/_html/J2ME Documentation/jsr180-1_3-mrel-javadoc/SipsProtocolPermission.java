package javax.microedition.sip;

import java.security.Permission;
import javax.microedition.io.GCFPermission;
import java.security.PermissionCollection;

/**
 * <p>This class represents access rights to connections via the sips protocol.
 * SipsProtocolPermission consists of a URI string but no actions list. The URI
 * string specifies a connection target and used protocol e.g. sip:user@172.21.38.219:5070.</p>
 *
 * <p>The URI is specified in RFC3261 and here it takes general form of:</p>
 * <p><code>{scheme}:{target}[{params}]</code></p>
 * <p>where:</p>
 * <ul>
 *	 <li><code>scheme</code> is SIP scheme supported by the system sip or sips</li>
 *	 <li><code>target</code> is user network address in form of [{user_name}@]{target_host}[:{port}] or {telephone_number}</li>
 * 	 <li><code>params</code> stands for additional SIP URI parameters like ;transport=udp</li>
 * </ul>
 * <p>The SIP URI may contain quote characters. The application can use either the quote
 *    character or it's escaped version (%22), the API implementation must support both forms.</p>
 * <p>Unlike in RFC3261, the {target} can here also be an asterisk ('*') to denote all valid SIP addresses.</p>
 *
 * @see javax.microedition.io.Connector#open(java.lang.String) MIDP 3.0 Specification
 */
public final class SipsProtocolPermission extends GCFPermission {

	/**
	 * <p>Creates a new SipsProtocolPermission instance with the specified URI as its name.</p>
	 *
	 * @param uri the URI string.
	 * @throws IllegalArgumentException if the <code>uri</code> is malformed
	 * @throws NullPointerException if <code>uri</code> is <code>null</code>
	 */
	public SipsProtocolPermission(String uri){}

	/**
	 * <p>Returns the protocol scheme of this SipsProtocolPermission</p>
     * @return the protocol scheme
     * @see javax.microedition.io.GCFPermission
	 */
	public String getProtocol(){return null;}

	/**
	 * <p>Returns the URI of this SipsProtocolPermission</p>
     * @return the URI
     * @see javax.microedition.io.GCFPermission
	 */
	public String getURI(){return null;}

	/**
	 * <p>Checks if this SipsProtocolPermission object implies
	 * the specified permission.</p>
	 *
	 * <p>Method returns true if:</p>
	 * <ul>
	 *	 <li><code>p</code> is an instance of SipsProtocolPermission and</li>
	 *	 <li>either <code>p's</code> URI equals this object's URI (Refer to the RFC3261 section 25 how the URIs are constructed.)</li>
	 *	 <li>or the <code>{target}</code> part of this object's URI is an asterisk ('*') and <code>p's</code> URI equals this object's URI for parts other than the <code>{target}</code></li>
	 * </ul>
	 *
	 * @param p the permission to check against.
	 * @return true if the specified permission is implied by this object,
	 * false if not.
	 */
	public boolean implies(Permission p) {return true;}

	/**
	 * <p>Checks two Permission objects for equality</p>
     * @param object the permission object to check equality with
     * @return true if obj is a SipsProtocolPermission and
	 *  has the same URI string as this SipsProtocolPermission object.
	 * @see java.security.Permission
	 */
	public boolean equals(Object object) {return false;}

	/**
	 * <p>Returns the actions as a String. Since the SipsProtocolPermission does
	 * 	  not have actions, method will always return empty string. </p>
	 * @return the empty string
	 * @see java.security.Permission
	 */
	public String getActions(){return "";}

	/**
	 * <p>Returns the hash code value for this Permission object</p>
	 * @return a hash code value for this object
	 * @see java.security.Permission
	 */
	public int hashCode() {return -1;}

    /**
	 * Returns a new <code>PermissionCollection</code> for storing
	 * <code>SipsProtocolPermission</code> objects.
	 *
	 * @return		a new <code>PermissionCollection</code>
	 *				suitable for storing <code>SipsProtocolPermission</code>
	 *				objects.
     */
	public PermissionCollection newPermissionCollection() {
		return (PermissionCollection) null;
	}
}