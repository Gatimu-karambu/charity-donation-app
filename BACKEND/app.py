from flask import Flask, request, jsonify, send_from_directory
from flask_sqlalchemy import SQLAlchemy
from flask_cors import CORS
from datetime import datetime
import logging
import re

app = Flask(__name__)
CORS(app)

# Set up logging
logging.basicConfig(
    level=logging.DEBUG,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = app.logger

# Database configuration
app.config['SQLALCHEMY_DATABASE_URI'] = 'mysql://root:mahlon_689@localhost/donation_db'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False

# Initialize SQLAlchemy
db = SQLAlchemy(app)

# Constants
PENDING = 'pending'
RECEIVED = 'received'
VALID_STATUSES = {PENDING, RECEIVED}

def format_phone(phone):
    """Format phone number to standard format (254XXXXXXXXX)"""
    if not phone:
        return None
    
    # Remove any non-digit characters
    cleaned = re.sub(r'\D', '', phone)
    
    # Remove leading zero if present
    if cleaned.startswith('0'):
        cleaned = cleaned[1:]
    
    # Add country code if not present
    if not cleaned.startswith('254'):
        cleaned = '254' + cleaned
    
    logger.debug(f"Formatted phone number: {phone} -> {cleaned}")
    return cleaned

class User(db.Model):
    __tablename__ = 'users'
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(100), nullable=False)
    phone = db.Column(db.String(15), unique=True, nullable=False)
    email = db.Column(db.String(100), unique=True, nullable=False)
    password = db.Column(db.String(255), nullable=False)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    donations = db.relationship('Donation', backref='user', lazy=True)

class Donation(db.Model):
    __tablename__ = 'donations'
    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column(db.Integer, db.ForeignKey('users.id'), nullable=False)
    donation_type = db.Column(db.String(50), nullable=False)
    amount = db.Column(db.Float)
    description = db.Column(db.Text)
    status = db.Column(db.String(20), nullable=False, default=PENDING)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)

# Routes
@app.route('/')
def serve_admin_panel():
    """Serve the admin panel HTML"""
    return send_from_directory('.', 'admin.html')

@app.route('/api/donate', methods=['POST'])
def add_donation():
    """Add a new donation"""
    try:
        data = request.json
        logger.debug(f"Received donation data: {data}")

        # Validate required fields
        if not data.get('phone'):
            return jsonify({
                "error": "Phone number is required",
                "received_data": data
            }), 400

        if not data.get('donationType'):
            return jsonify({
                "error": "Donation type is required",
                "received_data": data
            }), 400

        # Format phone number
        formatted_phone = format_phone(data['phone'])
        logger.debug(f"Formatted phone: {formatted_phone}")

        # Find or create user
        user = User.query.filter_by(phone=formatted_phone).first()
        if not user:
            logger.debug(f"Creating new user with phone: {formatted_phone}")
            user = User(
                name=data.get('name', 'Anonymous'),
                phone=formatted_phone,
                email=data.get('email', f"user_{formatted_phone}@example.com"),
                password='default_password'
            )
            db.session.add(user)
            db.session.commit()
            logger.debug(f"Created new user with ID: {user.id}")

        # Create donation
        donation = Donation(
            user_id=user.id,
            donation_type=data['donationType'],
            amount=data.get('amount'),
            description=data.get('description', ''),
            status=PENDING
        )
        
        db.session.add(donation)
        db.session.commit()
        logger.debug(f"Created donation: ID={donation.id}, Type={donation.donation_type}")

        return jsonify({
            "message": "Donation recorded successfully",
            "donation_id": donation.id,
            "user_id": user.id
        }), 201

    except Exception as e:
        logger.error(f"Error processing donation: {str(e)}")
        db.session.rollback()
        return jsonify({
            "error": str(e),
            "received_data": data if 'data' in locals() else None
        }), 500

def format_phone(phone):
    """Format phone number to standard format"""
    if not phone:
        return None
    
    # Remove all non-digit characters
    cleaned = re.sub(r'\D', '', phone)
    logger.debug(f"Cleaned phone number: {cleaned}")
    
    # If it starts with 254, keep as is
    if cleaned.startswith('254'):
        logger.debug(f"Phone already has country code: {cleaned}")
        return cleaned
        
    # If it starts with 0, replace with 254
    if cleaned.startswith('0'):
        cleaned = '254' + cleaned[1:]
        logger.debug(f"Replaced leading 0 with 254: {cleaned}")
        return cleaned
        
    # If it starts with 7 or 1, add 254
    if cleaned.startswith(('7', '1')):
        cleaned = '254' + cleaned
        logger.debug(f"Added country code 254: {cleaned}")
        return cleaned
        
    logger.warning(f"Unexpected phone format: {phone}")
    return cleaned

@app.route('/api/donations/<phone>', methods=['GET'])
def get_user_donations(phone):
    """Get donations for a specific user by phone number"""
    try:
        logger.debug(f"Received request for phone: {phone}")
        formatted_phone = format_phone(phone)
        logger.debug(f"Formatted phone number: {formatted_phone}")
        
        # Try different phone formats
        possible_formats = [
            formatted_phone,
            formatted_phone.lstrip('254'),  # Without country code
            '0' + formatted_phone.lstrip('254'),  # With leading zero
        ]
        
        logger.debug(f"Trying phone formats: {possible_formats}")
        
        # Find user with any of the phone formats
        user = None
        for phone_format in possible_formats:
            user = User.query.filter_by(phone=phone_format).first()
            if user:
                logger.debug(f"Found user with phone format: {phone_format}")
                break
                
        if not user:
            logger.warning(f"No user found for any format of phone: {phone}")
            return jsonify({
                "error": "User not found",
                "tried_formats": possible_formats
            }), 404

        # Get donations
        donations = Donation.query.filter_by(user_id=user.id)\
                                .order_by(Donation.created_at.desc())\
                                .all()
        
        logger.debug(f"Found {len(donations)} donations for user {user.name}")

        # Format response
        donation_list = [{
            'id': d.id,
            'donationType': d.donation_type,
            'amount': d.amount,
            'description': d.description,
            'status': d.status,
            'date': d.created_at.strftime('%Y-%m-%d %H:%M:%S'),
            'name': user.name,
            'phone': user.phone
        } for d in donations]

        return jsonify(donation_list), 200

    except Exception as e:
        logger.error(f"Error fetching donations: {str(e)}")
        return jsonify({
            "error": str(e),
            "phone": phone
        }), 500
@app.route('/api/donations/all', methods=['GET'])
def get_all_donations():
    """Get all donations for admin panel"""
    try:
        donations = Donation.query.order_by(Donation.created_at.desc()).all()
        logger.debug(f"Fetching all donations. Found: {len(donations)}")

        return jsonify([{
            'id': d.id,
            'name': d.user.name,
            'phone': d.user.phone,
            'email': d.user.email,
            'donationType': d.donation_type,
            'amount': d.amount,
            'description': d.description,
            'status': d.status,
            'date': d.created_at.strftime('%Y-%m-%d %H:%M:%S')
        } for d in donations]), 200

    except Exception as e:
        logger.error(f"Error fetching all donations: {str(e)}")
        return jsonify({"error": str(e)}), 500

@app.route('/api/donations/<int:donation_id>/status', methods=['PUT'])
def update_donation_status(donation_id):
    """Update donation status"""
    try:
        data = request.json
        logger.debug(f"Updating status for donation {donation_id}: {data}")

        if not data or 'status' not in data:
            return jsonify({"error": "Status is required"}), 400

        new_status = data['status'].lower()
        if new_status not in VALID_STATUSES:
            return jsonify({
                "error": f"Invalid status. Must be one of: {', '.join(VALID_STATUSES)}"
            }), 400

        donation = Donation.query.get_or_404(donation_id)
        old_status = donation.status
        donation.status = new_status
        db.session.commit()

        logger.debug(f"Updated donation {donation_id} status: {old_status} -> {new_status}")
        return jsonify({
            "message": "Status updated successfully",
            "donation_id": donation_id,
            "old_status": old_status,
            "new_status": new_status
        }), 200

    except Exception as e:
        logger.error(f"Error updating donation status: {str(e)}")
        db.session.rollback()
        return jsonify({"error": str(e)}), 500

@app.route('/api/cleanup-status', methods=['POST'])
def cleanup_status():
    """Cleanup invalid donation statuses"""
    try:
        # Update all statuses to be consistent
        affected = Donation.query.filter(~Donation.status.in_(VALID_STATUSES))\
                               .update({Donation.status: PENDING}, 
                                     synchronize_session=False)
        db.session.commit()
        logger.info(f"Cleaned up {affected} donation statuses")
        return jsonify({"message": f"Status cleanup completed. Updated {affected} records"}), 200

    except Exception as e:
        logger.error(f"Error cleaning up statuses: {str(e)}")
        db.session.rollback()
        return jsonify({"error": str(e)}), 500

@app.route('/api/test', methods=['GET'])
def test_connection():
    """Test endpoint to verify API is working"""
    return jsonify({"message": "API is working!"}), 200

if __name__ == '__main__':
    # Create all tables and perform initial cleanup
    with app.app_context():
        try:
            logger.info("Creating database tables...")
            db.create_all()
            
            # Update status column
            db.session.execute('ALTER TABLE donations MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT "pending"')
            db.session.commit()
            logger.info("Database setup completed successfully")
            
        except Exception as e:
            logger.warning(f"Database setup issue (might be already configured): {str(e)}")
            db.session.rollback()

    # Run the application
    app.run(debug=True, host='0.0.0.0', port=5000)