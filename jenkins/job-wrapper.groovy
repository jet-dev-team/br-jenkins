import org.apache.commons.io.FilenameUtils;

def vars = getVariables()

installAnsibleRequirements(vars.requirements_file, vars.ansible_collections)

def inventoryContent = getAnsibleInventoryContent(vars)

withEnv(["ANSIBLE_COLLECTIONS_PATH=${vars.ansible_collections}"]) {
  ansiblePlaybook(
    extraVars: vars,
    inventoryContent: inventoryContent,
    playbook: "${vars.base_path}/ansible/playbook.yml",
    credentialsId: "${vars.dest_credentials_id}",
    disableHostKeyChecking: true,
    colorized: true,
    extras: '-v'
  )
}

/*
 * Utility methods.
 */

/*
 * Returns a relative path to a directory where a Jenkinsfile is (without trailing slash).
 * The path is relative to a workspace.
 */
def getJobBasePath() {
  def scriptPath = currentBuild.rawBuild.parent.definition.scriptPath
  def path = FilenameUtils.getPathNoEndSeparator(scriptPath)
  return path
}

/*
 * Returns a map of required parameters (from env) and job's parameters combined.
 * All keys are in lower case.
 */
def getVariables() {
  def vars = [:]
  def envVars = env.getEnvironment()

  for (entry in Config.required_params.entrySet()) {
    def key = entry.key
    def defaultValue = entry.value

    // Check for value in envVars, then env.* and then default. 
    def envVarsValue = envVars[key]?.trim()
    def envValue = env."${key}"?.trim()
    vars[key.toLowerCase()] = envVarsValue ?: envValue ?: defaultValue

    // Error if neither is set (defaultValue is also empty)
    if (!vars[key.toLowerCase()]) {
      error("${key} is not set and has no default value. Aborting job...")
    }
  }

  vars.putAll(mapKeysToLowerCase(params))
  def basePath = getJobBasePath()
  vars['build_number'] = BUILD_NUMBER
  vars['base_path'] = basePath;
  vars['requirements_file'] = "${vars.base_path}/ansible/requirements.yml"
  vars['ansible_collections'] = "/tmp/ansible_collections/${envVars.JOB_NAME}"

  return vars
}

/*
 * Installs ansible-galaxy requirements if the corresponding file exists.
 */
def installAnsibleRequirements(requirementsFilePath, ansibleCollectionsPaths) {
  if (fileExists(requirementsFilePath)) {
    sh "ANSIBLE_COLLECTIONS_PATH=${ansibleCollectionsPaths} ansible-galaxy collection install -r ${requirementsFilePath} -p ${ansibleCollectionsPaths}"
  }
}

def getAnsibleInventoryContent(vars) {
  def hostVars = vars.findAll{it.key.startsWith('hv_')}
  def hostString = "[host]\n${vars.dest_host}"

  if (vars.dest_port) {
    hostString += " ansible_port=${vars.dest_port}"
  }

  if (vars.dest_user) {
    hostString += " ansible_user=${vars.dest_user}"
  }

  hostVars.each {
    hostString = hostString.concat(" ${it.key.replace("hv_", "")}=${it.value}")
  }

  // Force using ansible_python_interpreter.
  hostString += " ansible_python_interpreter=/usr/bin/python3"

  return hostString
}

/*
 * Returns a new map with keys in lower case.
 */
def mapKeysToLowerCase(map) {
  def lower = [:]
  map.each {
    lower[it.key.toLowerCase()] = it.value
  }

  return lower
}
