
pipeline {

    agent { label 'slave1' }
	
    tools {
        maven "maven-3.6.3"
    }

	environment {	
		DOCKERHUB_CREDENTIALS=credentials('dockerloginid')
	}
	
    stages {
        stage('SCM_Checkout') {
            steps {
                echo 'Perform SCM Checkout'
				git 'https://github.com/LoksaiETA/devops-java-webapp.git'
            }
			  post {
				success {
				  sh "echo 'Send mail on success'"
				  mail to:"loksaieta223@gmail.com", from: 'loksaieta223@gmail.com', subject:"SUCCES: ${currentBuild.fullDisplayName}", body: "SCM Checkout Succesful."
				}
			  }
        }
        stage('Application_Build') {
            steps {
                echo 'Perform Maven Build'
				sh 'mvn -Dmaven.test.failure.ignore=true clean package'
            }
        }
        stage('Build Docker Image') {
            steps {
				sh 'docker version'
				sh "docker build -t loksaieta/loksai-eta-app:${BUILD_NUMBER} ."
				sh 'docker image list'
				sh "docker tag loksaieta/loksai-eta-app:${BUILD_NUMBER} loksaieta/loksai-eta-app:latest"
            }
        }
        stage('Approve - Publish_to_Docker_Registry'){
            steps{
                
                //----------------send an approval prompt-------------
                script {
                   env.APPROVED_DEPLOY = input message: 'User input required Choose "Yes" | "Abort"'
                       }
                //-----------------end approval prompt------------
            }
        }
		stage('Login2DockerHub') {

			steps {
				sh 'echo $DOCKERHUB_CREDENTIALS_PSW | docker login -u $DOCKERHUB_CREDENTIALS_USR --password-stdin'
			}
		}
		stage('Publish_to_Docker_Registry') {
			steps {
				sh "docker push loksaieta/loksai-eta-app:latest"
			}
		}
        stage('Approve - Deployment'){
            steps{
                
                //----------------send an approval prompt-------------
                script {
                   env.APPROVED_DEPLOY = input message: 'User input required Choose "Yes" | "Abort"'
                       }
                //-----------------end approval prompt------------
            }
			  post {
				success {
				  sh "echo 'Send mail for Approval'"
				  mail to:"loksaieta223@gmail.com", from: 'loksaieta223@gmail.com', subject:"SUCCES: ${currentBuild.fullDisplayName}", body: "SCM Checkout Succesful."
				}
			  }
        }
		stage('Deploy to Kubernetes') {
            steps {
				script {
				sshPublisher(publishers: [sshPublisherDesc(configName: 'Kubernetes_Cluster', transfers: [sshTransfer(cleanRemote: false, excludes: '', execCommand: 'kubectl apply -f k8smvndeployment.yaml', execTimeout: 120000, flatten: false, makeEmptyDirs: false, noDefaultExcludes: false, patternSeparator: '[, ]+', remoteDirectory: '.', remoteDirectorySDF: false, removePrefix: '', sourceFiles: '*.yaml')], usePromotionTimestamp: false, useWorkspaceInPromotion: false, verbose: false)])
			}	
            }
		}
    }
}
