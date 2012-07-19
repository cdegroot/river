/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
* @author Alexey V. Varlamov
* @version $Revision$
*/

package org.apache.river.api.security;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import org.apache.river.impl.net.UriString;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.AccessController;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Permission;
import java.security.Principal;
import java.security.UnresolvedPermission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.river.api.security.DefaultPolicyScanner.GrantEntry;
import org.apache.river.api.security.DefaultPolicyScanner.KeystoreEntry;
import org.apache.river.api.security.DefaultPolicyScanner.PermissionEntry;
import org.apache.river.api.security.DefaultPolicyScanner.PrincipalEntry;
import org.apache.river.api.security.PolicyUtils.ExpansionFailedException;


/**
 * This is a basic loader of policy files. It delegates lexical analysis to 
 * a pluggable scanner and converts received tokens to a set of 
 * {@link org.apache.river.security.PermissionGrant PermissionGrant's}. 
 * For details of policy format, which should be identical to Sun's Java Policy
 * files see the 
 * {@link org.apache.river.imp.security.policy.se.ConcurrentPolicyFile default policy description}.
 * <br>
 * For ordinary uses, this class has just one public method <code>parse()</code>, 
 * which performs the main task.
 * Extensions of this parser may redefine specific operations separately, 
 * by overriding corresponding protected methods. 
 * <br>
 * This implementation is effectively thread-safe, as it has no field references 
 * to data being processed (that is, passes all the data as method parameters).
 * 
 * @see org.apache.river.imp.security.policy.se.ConcurrentPolicyFile
 * @see org.apache.river.imp.security.policy.util.DefaultPolicyScanner
 * @see org.apache.river.api.security.PermissionGrant
 */
class DefaultPolicyParser implements PolicyParser {

    // Pluggable scanner for a specific file format
    private final DefaultPolicyScanner scanner;

    /** 
     * Default constructor, 
     * {@link org.apache.river.imp.security.policy.util.DefaultPolicyScanner DefaultPolicyScanner} 
     * is used. 
     */
    DefaultPolicyParser() {
        scanner = new DefaultPolicyScanner();
    }

    /** 
     * Extension constructor for plugging-in custom scanner.
     */
    DefaultPolicyParser(DefaultPolicyScanner s) {
        this.scanner = s;
    }

    /**
     * This is the main business method. It manages loading process as follows:
     * the associated scanner is used to parse the stream to a set of 
     * {@link org.apache.river.imp.security.policy.util.DefaultPolicyScanner.GrantEntry composite tokens},
     * then this set is iterated and each token is translated to a PermissionGrant.
     * Semantically invalid tokens are ignored, the same as void PermissionGrant's.
     * <br>
     * A policy file may refer to some KeyStore(s), and in this case the first
     * valid reference is initialized and used in processing tokens.   
     * 
     * @param location an URL of a policy file to be loaded
     * @param system system properties, used for property expansion
     * @return a collection of PermissionGrant objects, may be empty
     * @throws Exception IO error while reading location or file syntax error 
     */
    public Collection<PermissionGrant> parse(URL location, Properties system)
            throws Exception {
        boolean resolve = PolicyUtils.canExpandProperties();
        Reader r = new BufferedReader(new InputStreamReader(
                AccessController
                        .doPrivileged(new PolicyUtils.URLLoader(location))));

        Collection<GrantEntry> grantEntries = new HashSet<GrantEntry>();
        List<KeystoreEntry> keystores = new ArrayList<KeystoreEntry>();

        try {
            scanner.scanStream(r, grantEntries, keystores); // modifies keystores
        }
        finally {
            r.close();
        }

        //XXX KeyStore could be loaded lazily...
        KeyStore ks = initKeyStore(keystores, location, system, resolve);

        Collection<PermissionGrant> result = new HashSet<PermissionGrant>();
        for (Iterator<GrantEntry> iter = grantEntries.iterator(); iter.hasNext();) {
            DefaultPolicyScanner.GrantEntry ge = iter.next();
            try {
                PermissionGrant pe = resolveGrant(ge, ks, system, resolve);
                if (!pe.isVoid()) {
                    result.add(pe);
                }
            }
            catch (Exception e) {
                if ( e instanceof SecurityException ) throw (SecurityException) e;
                System.err.println("Problem parsing policy: "+ location 
                        + "\n" + e);
            }
        }
        
        return result;
    }

    /**
     * Translates GrantEntry token to PermissionGrant object. It goes step by step, 
     * trying to resolve each component of the GrantEntry:
     * <ul>
     * <li> If <code>codebase</code> is specified, expand it and construct an URL.
     * <li> If <code>signers</code> is specified, expand it and obtain 
     * corresponding Certificates.
     * <li> If <code>principals</code> collection is specified, iterate over it. 
     * For each PrincipalEntry, expand name and if no class specified, 
     * resolve actual X500Principal from a KeyStore certificate; otherwise keep it 
     * as UnresolvedPrincipal. 
     * <li> Iterate over <code>permissions</code> collection. For each PermissionEntry,
     * try to resolve (see method 
     * {@link #resolvePermission(DefaultPolicyScanner.PermissionEntry, DefaultPolicyScanner.GrantEntry, KeyStore, Properties, boolean) resolvePermission()}) 
     * a corresponding permission. If resolution failed, ignore the PermissionEntry. 
     * </ul>
     * In fact, property expansion in the steps above is conditional and is ruled by
     * the parameter <i>resolve</i>.  
     * <br>
     * Finally a new PermissionGrant is created, which associates the trinity 
     * of resolved URL, Certificates and Principals to a set of granted Permissions.
     * 
     * @param ge GrantEntry token to be resolved
     * @param ks KeyStore for resolving Certificates, may be <code>null</code> 
     * @param system system properties, used for property expansion 
     * @param resolve flag enabling/disabling property expansion
     * @return resolved PermissionGrant
     * @throws Exception if unable to resolve codebase, signers or principals 
     * of the GrantEntry
     * @see DefaultPolicyScanner.PrincipalEntry
     * @see DefaultPolicyScanner.PermissionEntry
     * @see org.apache.river.imp.security.policy.util.PolicyUtils
     */
    PermissionGrant resolveGrant(DefaultPolicyScanner.GrantEntry ge,
            KeyStore ks, Properties system, boolean resolve) throws Exception {
        if ( ge == null ) return null;
        /*
         * Do we return multiple grants or do we allow a codebase array 
         * in a permission grant?
         * 
         * ANSWER: No we just make a CodeSourceSetGrant, that contains multiple
         * CodeSource.
         */
        List<URI> codebases = new ArrayList<URI>(8);
        Certificate[] signers = null;
        Set<Principal> principals = new HashSet<Principal>();
        Set<Permission> permissions = new HashSet<Permission>();
        String cb = ge.getCodebase(null);
        String signerString = ge.getSigners();
        if ( cb != null ) {
            if ( resolve ) {
                try {
                    Collection<String> cbstr = expandURLs(cb, system);
                    Iterator<String> it = cbstr.iterator();
                    while (it.hasNext()){
                        codebases.add(getURI(it.next()));
                    }
                } catch (ExpansionFailedException e) {
                    codebases.add(getURI(cb));
                }
            } else {
                codebases.add(getURI(cb));
            }
        }
        if ( signerString != null) {
            if (resolve) {
                signerString = PolicyUtils.expand(signerString, system);
            }
            signers = resolveSigners(ks, signerString);
        }
        if (ge.getPrincipals(null) != null) {
            String principalName = null;
            String principalClass = null;
            for (Iterator<PrincipalEntry> iter = ge.getPrincipals(system).iterator(); iter.hasNext();) {
                DefaultPolicyScanner.PrincipalEntry pe = iter.next();
                principalName = pe.getName();
                principalClass = pe.getKlass();
                if (resolve) {
                    principalName = PolicyUtils.expand(principalName, system);
                }
                if (principalClass == null) {
                    principals.add(getPrincipalByAlias(ks, principalName));
                } else {
                    principals.add(new UnresolvedPrincipal(principalClass, principalName));
                }
            }
        }
        Collection<PermissionEntry> pec = ge.getPermissions();
        if (pec != null) {
            Iterator<PermissionEntry> iter = pec.iterator();
            while ( iter.hasNext()) {
                DefaultPolicyScanner.PermissionEntry pe = iter.next();
                try {
                    permissions.add(resolvePermission(pe, ge, ks, system, resolve));
                }
                catch (Exception e) {
                    if ( e instanceof SecurityException ) throw (SecurityException) e;
                    // TODO: log warning.
                    System.err.println(e);
                }
            }
        }
        PermissionGrantBuilder pgb = PermissionGrantBuilder.newBuilder();
        Iterator<URI> iter = codebases.iterator();
        while (iter.hasNext()){
            pgb.uri(iter.next());
        }
        return pgb
            .certificates(signers)
            .principals(principals.toArray(new Principal[principals.size()]))
            .permissions(permissions.toArray(new Permission[permissions.size()]))
            .context(PermissionGrantBuilder.URI)
            .build();
    }
    
    URI getURI(String uriString) throws MalformedURLException, URISyntaxException{
        // We do this to support windows, this is to ensure that drive letter
        // capitalisation is correct and illegal strings are escaped correctly.
        String result = UriString.parse(uriString);
//        boolean isFile = result.startsWith("file:") || result.startsWith("FILE:");
        URI uri = new URI(result);
        // Oddly enough, this next part didn't quite work out as expected.
//        if (isFile){
//            if ( result.endsWith("*") || result.endsWith("-")){
//                // We did our best to create a compatible URI,
//                // upper and lower case may cause issues on windows, if the
//                // CodeSource url differs.
//                return uri; 
//            } else {
//                // Since file is system dependant, it will guarantee that
//                // the path is compatible and exists.
//                File file = new File(uri);
//                return file.toURI().normalize();
//            }
//        }
        return uri;
    }
    
    Segment segment(String s, Properties p) throws ExpansionFailedException{
        final String ARRAY_START_MARK = "${{";
        final String ARRAY_END_MARK = "}}";
        final String ARRAY_SEPARATOR = p.getProperty("path.separator");
        final String START_MARK = "${"; //$NON-NLS-1$
        final String END_MARK = "}"; //$NON-NLS-1$
        Segment primary = new Segment(s, null);
        primary.divideAndReplace(ARRAY_START_MARK, ARRAY_END_MARK,
                ARRAY_SEPARATOR, p);
        primary.divideAndReplace(START_MARK, END_MARK, null, p);
        // Repeat twice for nested properties
        primary.divideAndReplace(START_MARK, END_MARK, null, p);
        primary.divideAndReplace(START_MARK, END_MARK, null, p);
        return primary;
    }
    
    Collection<String> expandURLs(String s, Properties p) throws ExpansionFailedException{
        Segment seg = segment(s,p);
        Collection<String> urls = new ArrayList<String>();
        while ( seg.hasNext() ){
//            urls.add(seg.next().replace(File.separatorChar, '/'));
            urls.add(seg.next());
        }
        return urls;
    }   

    /**
     * Translates PermissionEntry token to Permission object.
     * First, it performs general expansion for non-null <code>name</code> and
     * properties expansion for non-null <code>name</code>, <code>action</code> 
     * and <code>signers</code>.
     * Then, it obtains signing Certificates(if any), tries to find a class specified by 
     * <code>klass</code> name and instantiate a corresponding permission object.
     * If class is not found or it is signed improperly, returns UnresolvedPermission.
     *
     * @param pe PermissionEntry token to be resolved
     * @param ge parental GrantEntry of the PermissionEntry 
     * @param ks KeyStore for resolving Certificates, may be <code>null</code>
     * @param system system properties, used for property expansion
     * @param resolve flag enabling/disabling property expansion
     * @return resolved Permission object, either of concrete class or UnresolvedPermission
     * @throws Exception if failed to expand properties, 
     * or to get a Certificate, 
     * or to newBuilder an instance of a successfully found class 
     */
    Permission resolvePermission(
            DefaultPolicyScanner.PermissionEntry pe,
            DefaultPolicyScanner.GrantEntry ge, KeyStore ks, Properties system,
            boolean resolve) throws Exception {
        String className = pe.getKlass(), name=pe.getName(), 
                actions=pe.getActions(), signer=pe.getSigners();
        if (name != null) {
            name = PolicyUtils.expandGeneral(name, new PermissionExpander(ge, ks));
        }
        if (resolve) {
            if (name != null) {
                name = PolicyUtils.expand(name, system);
            }
            if (actions != null) {
                actions = PolicyUtils.expand(actions, system);
            }
            if (signer != null) {
                signer = PolicyUtils.expand(signer, system);
            }
        }
        Certificate[] signers = (signer == null) ? null : resolveSigners(
                ks, signer);
        try {
            Class<?> klass = Class.forName(className);
            if (PolicyUtils.matchSubset(signers, klass.getSigners())) {
                return PolicyUtils.instantiatePermission(klass, name, actions);
            }
        }
        catch (ClassNotFoundException cnfe) {}
        //maybe properly signed class will be loaded later
        return new UnresolvedPermission(className, name, actions, signers);
    }

    /** 
     * Specific handler for expanding <i>self</i> and <i>alias</i> protocols. 
     */
    class PermissionExpander implements PolicyUtils.GeneralExpansionHandler {

        // Store KeyStore
        private final KeyStore ks;

        // Store GrantEntry
        private final DefaultPolicyScanner.GrantEntry ge;

        /** 
         * Combined setter of all required fields. 
         */
        PermissionExpander(DefaultPolicyScanner.GrantEntry ge,
                KeyStore ks) {
            this.ge = ge;
            this.ks = ks;
        }

        /**
         * Resolves the following protocols:
         * <dl>
         * <dt>self
         * <dd>Denotes substitution to a principal information of the parental 
         * GrantEntry. Returns a space-separated list of resolved Principals 
         * (including wildcarded), formatting each as <b>class &quot;name&quot;</b>.
         * If parental GrantEntry has no Principals, throws ExpansionFailedException.
         * <dt>alias:<i>name</i>
         * <dd>Denotes substitution of a KeyStore alias. Namely, if a KeyStore has 
         * an X.509 certificate associated with the specified name, then returns 
         * <b>javax.security.auth.x500.X500Principal &quot;<i>DN</i>&quot;</b> string, 
         * where <i>DN</i> is a certificate's subject distinguished name.  
         * </dl>
         * @throws ExpansionFailedException - if protocol is other than 
         * <i>self</i> or <i>alias</i>, or if data resolution failed 
         */
        public String resolve(String protocol, String data)
                throws PolicyUtils.ExpansionFailedException {

            if ("self".equals(protocol)) { //$NON-NLS-1$
                //need expanding to list of principals in grant clause 
                if (ge.getPrincipals(null) != null && ge.getPrincipals(null).size() != 0) {
                    StringBuilder sb = new StringBuilder();
                    for (Iterator<PrincipalEntry> iter = ge.getPrincipals(null).iterator(); iter
                            .hasNext();) {
                        DefaultPolicyScanner.PrincipalEntry pr = iter
                                .next();
                        if (pr.getKlass() == null) {
                            // aliased X500Principal
                            try {
                                sb.append(pc2str(getPrincipalByAlias(ks, pr.getName())));
                            }
                            catch (Exception e) {
                                if ( e instanceof SecurityException ) throw (SecurityException) e;
                                throw new PolicyUtils.ExpansionFailedException(
                                        Messages.getString("security.143", pr.getName()), e); //$NON-NLS-1$
                            }
                        } else {
                            sb.append(pr.getKlass()).append(" \"").append(pr.getName()) //$NON-NLS-1$
                                    .append("\" "); //$NON-NLS-1$
                        }
                    }
                    return sb.toString();
                } else {
                    throw new PolicyUtils.ExpansionFailedException(
                            Messages.getString("security.144")); //$NON-NLS-1$
                }
            }
            if ("alias".equals(protocol)) { //$NON-NLS-1$
                try {
                    return pc2str(getPrincipalByAlias(ks, data));
                }
                catch (Exception e) {
                    if ( e instanceof SecurityException ) throw (SecurityException) e;
                    throw new PolicyUtils.ExpansionFailedException(
                            Messages.getString("security.143", data), e); //$NON-NLS-1$
                }
            }
            throw new PolicyUtils.ExpansionFailedException(
                    Messages.getString("security.145", protocol)); //$NON-NLS-1$
        }

        // Formats a string describing the passed Principal. 
        private String pc2str(Principal pc) {
            String klass = pc.getClass().getName();
            String name = pc.getName();
            StringBuilder sb = new StringBuilder(klass.length() + name.length()
                    + 5);
            return sb.append(klass).append(" \"").append(name).append("\"") //$NON-NLS-1$ //$NON-NLS-2$
                    .toString();
        }
    }

    /**
     * Takes a comma-separated list of aliases and obtains corresponding 
     * certificates.
     * @param ks KeyStore for resolving Certificates, may be <code>null</code> 
     * @param signers comma-separated list of certificate aliases, 
     * must be not <code>null</code>
     * @return an array of signing Certificates
     * @throws Exception if KeyStore is <code>null</code> 
     * or if it failed to provide a certificate  
     */
    Certificate[] resolveSigners(KeyStore ks, String signers)
            throws Exception {
        if (ks == null) {
            throw new KeyStoreException(Messages.getString("security.146", //$NON-NLS-1$
                    signers));
        }

        Collection<Certificate> certs = new HashSet<Certificate>();
        StringTokenizer snt = new StringTokenizer(signers, ","); //$NON-NLS-1$
        while (snt.hasMoreTokens()) {
            //XXX cache found certs ??
            certs.add(ks.getCertificate(snt.nextToken().trim()));
        }
        return certs.toArray(new Certificate[certs.size()]);
    }

    /**
     * Returns a subject's X500Principal of an X509Certificate, 
     * which is associated with the specified keystore alias. 
     * @param ks KeyStore for resolving Certificate, may be <code>null</code>
     * @param alias alias to a certificate
     * @return X500Principal with a subject distinguished name
     * @throws KeyStoreException if KeyStore is <code>null</code> 
     * or if it failed to provide a certificate
     * @throws CertificateException if found certificate is not 
     * an X509Certificate 
     */
    Principal getPrincipalByAlias(KeyStore ks, String alias)
            throws KeyStoreException, CertificateException {

        if (ks == null) {
            throw new KeyStoreException(
                    Messages.getString("security.147", alias)); //$NON-NLS-1$
        }
        //XXX cache found certs ??
        Certificate x509 = ks.getCertificate(alias);
        if (x509 instanceof X509Certificate) {
            return ((X509Certificate) x509).getSubjectX500Principal();
        } else {
            throw new CertificateException(Messages.getString("security.148", //$NON-NLS-1$
                    alias, x509));
        }
    }

    /**
     * Returns the first successfully loaded KeyStore, from the specified list of
     * possible locations. This method iterates over the list of KeystoreEntries;
     * for each entry expands <code>url</code> and <code>type</code>,
     * tries to construct instances of specified URL and KeyStore and to load 
     * the keystore. If it is loaded, returns the keystore, otherwise proceeds to 
     * the next KeystoreEntry. 
     * <br>
     * <b>Note:</b> an url may be relative to the policy file location or absolute.
     * @param keystores list of available KeystoreEntries
     * @param base the policy file location
     * @param system system properties, used for property expansion
     * @param resolve flag enabling/disabling property expansion
     * @return the first successfully loaded KeyStore or <code>null</code>
     */
    KeyStore initKeyStore(List<KeystoreEntry>keystores,
            URL base, Properties system, boolean resolve) {
        for (int i = 0; i < keystores.size(); i++) {
            try {
                DefaultPolicyScanner.KeystoreEntry ke = keystores.get(i);
                String url = ke.getUrl(), type = ke.getType();
                if (resolve) {
                    url = PolicyUtils.expandURL(url, system);
                    if (type != null) {
                        type = PolicyUtils.expand(type, system);
                    }
                }
                if (type == null || type.length() == 0) {
                    type = KeyStore.getDefaultType();
                }
                KeyStore ks = KeyStore.getInstance(type);
                URL location = new URL(base, url);
                InputStream is = AccessController
                        .doPrivileged(new PolicyUtils.URLLoader(location));
                try {
                    ks.load(is, null);
                }
                finally {
                    is.close();
                }
                return ks;
            }
            catch (Exception e) {
                if ( e instanceof SecurityException ) throw (SecurityException) e;
                e.printStackTrace(System.err);
                // TODO: log warning
            }
        }
        return null;
    }
}
