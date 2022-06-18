# Sendgrid mail from mailx


send mail from bash using mailx and sendgrid
---

mailx -s "$SUBJECT" -S smtp="smtp://smtp.sendgrid.net:587" -S smtp-auth=login -S smtp-auth-user=apikey -S smtp-auth-password=your_api_key -S from="from@mail.adress"  "someone@somewhere.com" 
