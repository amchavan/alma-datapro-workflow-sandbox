from setuptools import setup

dependencies = [
    "python-jose",
    "pika",
    "lxml"
]

setup(name='adaptmb',
      version='0.1',
      description='ADAPT Python Message Bus API',
      url='http://www.almaobservatory.org',
      author='ALMA',
      author_email='test@alma.cl',
      license='LGPL',
      packages=['adapt', 'adapt/messagebus', 'adapt/messagebus/rabbitmq', 'adapt/messagebus/security', 'adapt/messagebus/configuration', 'adapt/resources'],
      install_requires=dependencies,
      include_package_data=True,
      zip_safe=False)
