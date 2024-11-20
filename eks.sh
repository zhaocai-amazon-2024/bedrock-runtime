# 创建 IAM 策略
cat <<EOF > bedrock-policy.json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "bedrock:*",
                "sts:AssumeRole"
            ],
            "Resource": "*"
        }
    ]
}
EOF

# 创建 IAM policy
aws iam create-policy \
    --policy-name bedrock-policy-new \
    --policy-document file://bedrock-policy.json

# 创建 Kubernetes 服务账号
kubectl create serviceaccount bedrock-sa

# 为 EKS 创建 IAM 角色并关联服务账号
eksctl create iamserviceaccount \
    --name bedrock-sa \
    --namespace default \
    --cluster jlc-cluster \
    --attach-policy-arn arn:aws:iam::017820696633:policy/bedrock-policy-new \
    --approve \
    --override-existing-serviceaccounts


# # 删除现有部署
# kubectl delete deployment bedrock-runtime

# # 应用新配置
# kubectl apply -f deployment.yaml

# # 查看 pod 状态
# kubectl get pods

# # 查看详细日志
# kubectl logs -l app=bedrock-runtime --tail=100 -f

#kubectl describe pod bedrock-runtime-6dfdb879c8-7lslg

#删除后会自动重启
#kubectl delete pod bedrock-runtime-6dfdb879c8-9mpbl 