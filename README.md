# tl500-teamsters

A Quarkus Qute, k8s-api based application to help "fast forward" TL500 teams. 

![images/teamster-intro.png](images/teamster-intro.png)

![images/teamster-nuke.png](images/teamster-nuke.png)

![images/teamster-create.png](images/teamster-create.png)

## Configuration

```bash
# set the following env vars
export CLUSTER_DOMAIN=         # cluster base domain (no https://)
export GIT_SERVER=             # git server (no https://)
export TEAM_NAME=              # team name [a-bA-B0-9-]
export GITLAB_USER=            # team gitlab username (same as ocp username)
export GITLAB_PASSWORD=        # team gitlab password for user
export OCP_ADMIN_USER=         # ocp cluster-admin user
export OCP_ADMIN_PASSWORD=     # ocp cluster-admin password
```

## Run

```bash
mvn quarkus:dev
```

## OpenShift / k8s

Add helm chart repo

```bash
helm repo add tl500-teamsters https://rht-labs.com/tl500-teamsters
```

Export the env.vars and use helm to install:

```bash
helm upgrade --install tl500-teamsters tl500-teamsters/tl500-teamsters --namespace tl500-teamsters --create-namespace \
  --set=clusterDomain=${CLUSTER_DOMAIN} \
  --set=gitServer=${GIT_SERVER} \
  --set=gitlabUser=${GITLAB_USER} \
  --set=gitlabPassword=${GITLAB_PASSWORD} \
  --set=ocpAdminUser=${OCP_ADMIN_USER} \
  --set=ocpAdminPassword=${OCP_ADMIN_PASSWORD}
```

## Signature

The public key of [tl500-teamsters images](https://quay.io/repository/rht-labs/tl500-teamsters)

[Cosign](https://github.com/sigstore/cosign) public key:

```shell
-----BEGIN PUBLIC KEY-----
MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE2N/IBzdLXjSh9+lKZr70j0KYcSIJ
6r0JNpf3Ul77AjF127OmpqRqsPDjvOr1YzLVPfR06Ll1Sa2hPXPVefffeg==
-----END PUBLIC KEY-----
```

The public key is also available online: <https://raw.githubusercontent.com/rht-labs/tl500-teamsters/master/cosign.pub>

To verify an image:

```shell
curl --progress-bar -o cosign.pub https://raw.githubusercontent.com/rht-labs/tl500-teamsters/master/cosign.pub
cosign verify --key cosign.pub quay.io/rht-labs/tl500-teamsters:latest
```
